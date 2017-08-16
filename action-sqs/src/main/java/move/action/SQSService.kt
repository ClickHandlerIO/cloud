package move.action

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.ExecutorFactory
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.http.AmazonHttpClient
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.transfer.*
import com.amazonaws.services.s3.transfer.internal.AbstractTransfer
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder
import com.amazonaws.services.sqs.buffered.MoveAmazonSQSBufferedAsyncClient
import com.amazonaws.services.sqs.buffered.QueueBuffer
import com.amazonaws.services.sqs.buffered.QueueBufferConfig
import com.amazonaws.services.sqs.model.*
import com.codahale.metrics.Counter
import com.codahale.metrics.Gauge
import com.google.common.base.Preconditions
import com.google.common.base.Strings
import com.google.common.base.Throwables
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.RemovalListener
import com.google.common.cache.RemovalNotification
import com.google.common.collect.ImmutableSet
import com.google.common.util.concurrent.AbstractIdleService
import com.google.common.util.concurrent.Service
import com.netflix.hystrix.exception.HystrixTimeoutException
import io.vertx.rxjava.core.TimeoutStream
import io.vertx.rxjava.core.Vertx
import javaslang.control.Try
import move.cluster.HazelcastProvider
import move.common.Metrics
import move.common.UID
import move.common.WireFormat
import org.slf4j.LoggerFactory
import rx.Single
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the lifecycle of all SQS Producers and Consumers.

 * @author Clay Molocznik
 */
@Singleton
class SQSService @Inject
internal constructor(val vertx: Vertx,
                     val config: SQSConfig,
                     val hazelcastProvider: HazelcastProvider) : AbstractIdleService(), WorkerService {

   private val queueMap = HashMap<String, QueueContext>()

   private lateinit var realSQS: AmazonSQSAsync
   private lateinit var client: MoveAmazonSQSBufferedAsyncClient
   private lateinit var bufferExecutor: ExecutorService
   //   private lateinit var s3Client: AmazonS3Client
   private lateinit var s3TransferManager: TransferManager

   private val activeFiles: Cache<String, File> = CacheBuilder
      .newBuilder()
      .maximumSize(MAX_FILE_CACHE)
      .expireAfterWrite(MAX_CACHE_AGE_MINUTES, TimeUnit.MINUTES)
      .removalListener(object : RemovalListener<String, File> {
         override fun onRemoval(notification: RemovalNotification<String, File>?) {
            if (notification?.wasEvicted() == true) {
               deleteFile(notification.key, notification.value)
            }
         }
      }).build()

   private val needToDeleteFiles: Cache<String, File> = CacheBuilder
      .newBuilder()
      .maximumSize(MAX_FILE_CACHE)
      .expireAfterWrite(FILE_RETRY_AGE_SECONDS, TimeUnit.MINUTES)
      .removalListener(object : RemovalListener<String, File> {
         override fun onRemoval(notification: RemovalNotification<String, File>?) {
            if (notification?.wasEvicted() == true) {
               deleteFile(notification.key, notification.value)
            }
         }
      }).build()

   private val activeUploads: Cache<String, Upload> = CacheBuilder
      .newBuilder()
      .maximumSize(MAX_UPLOADS)
      .expireAfterWrite(MAX_CACHE_AGE_MINUTES, TimeUnit.MINUTES)
      .removalListener(object : RemovalListener<String, Upload> {
         override fun onRemoval(notification: RemovalNotification<String, Upload>?) {
            if (notification?.wasEvicted() == true) {
               try {
                  notification.value.abort()
               } catch (e: Throwable) {
               }
            }
         }
      }).build()

   private val activeDownloads: Cache<String, Download> = CacheBuilder
      .newBuilder()
      .maximumSize(MAX_DOWNLOADS)
      .expireAfterWrite(MAX_CACHE_AGE_MINUTES, TimeUnit.MINUTES)
      .removalListener(object : RemovalListener<String, Download> {
         override fun onRemoval(notification: RemovalNotification<String, Download>?) {
            if (notification?.wasEvicted() == true) {
               try {
                  notification.value.abort()
               } catch (e: Throwable) {
               }
            }
         }
      }).build()

   private val fileSystem = vertx.fileSystem()
   private lateinit var timer: TimeoutStream

   /**
    * Determines if this instance allows for "any" worker actions.
    */
   val isWorkerEnabled: Boolean
      get() = config != null && config.worker != null && config.worker == true

   private fun deleteFile(key: String, file: File) {
      if (Vertx.currentContext().isEventLoopContext) {
         fileSystem.rxExists(file.absolutePath).subscribe(
            {
               if (it) {
                  fileSystem.rxDelete(file.absolutePath).subscribe(
                     {
                     },
                     {
                        try {
                           LOG.error("Failed to delete temporary file '" + file.absolutePath +
                              "' Going to put back in the cache and try again later.", it)
                        } finally {
                           needToDeleteFiles.put(key, file)
                        }
                     }
                  )
               }
            },
            {
               try {
                  LOG.error("Failed to delete temporary file '" + file.absolutePath +
                     "' Going to put back in the cache and try again later.", it)
               } finally {
                  needToDeleteFiles.put(key, file)
               }
            }
         )
      } else {
         if (file.exists()) {
            if (!file.delete()) {
               needToDeleteFiles.put(key, file)
            }
         }
      }
   }

   private fun workerEnabled(provider: WorkerActionProvider<*, *>): Boolean {
      if (config.worker == null || config.worker == false) {
         return false
      }

      if (config.exclusions != null && !config.exclusions!!.isEmpty()) {
         return !config.exclusions!!.contains(provider.name)
            && !config.exclusions!!.contains(provider.queueName)
      }

      if (config.inclusions != null && !config.inclusions!!.isEmpty()) {
         return config.inclusions!!.contains(provider.name)
            || config.inclusions!!.contains(provider.queueName)
      }

      return true
   }

   private fun queueConfig(queueName: String): SQSQueueConfig {
      var queueConfig = queueConfig0(queueName)

      if (queueConfig.maxBatchSize < 1) {
         queueConfig.maxBatchSize = config.maxBatchSize
      }

      if (queueConfig.maxBatchOpenMs < 0) {
         queueConfig.maxBatchOpenMs = config.maxBatchOpenMs
      }

      if (queueConfig.maxInflightReceiveBatches < 0) {
         queueConfig.maxInflightReceiveBatches = config.maxInflightReceiveBatches
      }

      if (queueConfig.maxDoneReceiveBatches < 0) {
         queueConfig.maxDoneReceiveBatches = config.maxDoneReceiveBatches
      }

      return queueConfig
   }

   private fun queueConfig0(queueName: String): SQSQueueConfig {
      if (config.queues == null || config.queues!!.isEmpty()) {
         return SQSQueueConfig()
      }

      return config.queues!!.stream()
         .filter {
            Strings.nullToEmpty(it.name).equals(queueName, ignoreCase = true) ||
               Strings.nullToEmpty(it.name).equals(queueName, ignoreCase = true)
         }
         .findFirst()
         .orElse(null)
   }

   /**
    * We need daemon threads in our executor so that we don't keep the process running if our
    * executor threads are the only ones left in the process.
    */
   private class DaemonThreadFactory : ThreadFactory {

      override fun newThread(r: Runnable): Thread {
         val threadNumber = threadCount.addAndGet(1)
         val thread = Thread(r)
         thread.isDaemon = true
         thread.name = "SQSQueueBufferWorkerThread-" + threadNumber
         return thread
      }

      companion object {
         internal var threadCount = AtomicInteger(0)
      }

   }

   @Throws(Exception::class)
   override fun startUp() {
      Preconditions.checkNotNull<SQSConfig>(config, "config must be set.")

      // AmazonHttpClient is vary chatty with log output. Shut it up.
      val amazonClientLogger = LoggerFactory.getLogger(AmazonHttpClient::class.java)
      Try.run {
         val param = Class.forName("ch.qos.logback.classic.Level")
         val errorField = param.getDeclaredField("ERROR")
         val method = amazonClientLogger.javaClass.getMethod("setLevel", param)
         val value = errorField.get(param)
         method?.invoke(amazonClientLogger, value)
      }

      // Sanitize namespace property.
      config.namespace = config.namespace?.trim() ?: ""

      if (config.worker == null) {
         config.worker = false
      }

      val numberOfQueues = ActionManager.workerActionQueueGroupMap.size

      // Create SQS QueueBuffer Executor.
      // Uses a cached thread strategy with the core size set to 0.
      // So it won't consume any threads if there isn't any activity.
      bufferExecutor = ThreadPoolExecutor(
         0,
         Integer.MAX_VALUE,
         KEEP_ALIVE_SECONDS,
         TimeUnit.SECONDS,
         SynchronousQueue<Runnable>(),
         DaemonThreadFactory()
      )

      val builder = AmazonSQSAsyncClientBuilder
         .standard()
         .withExecutorFactory {
            bufferExecutor
         }
         .withRegion(config.region)

      // Clean config values
      config.awsAccessKey = config.awsAccessKey?.trim() ?: ""
      config.awsSecretKey = config.awsSecretKey?.trim() ?: ""
      config.s3AccessKey = config.s3AccessKey?.trim() ?: ""
      config.s3SecretKey = config.s3SecretKey?.trim() ?: ""

      // Build S3 TransferManager
      s3TransferManager = TransferManagerBuilder.standard().apply {
         val clientBuilder = AmazonS3ClientBuilder.standard()

         if (config.s3AccessKey != null && config.s3AccessKey!!.isNotEmpty()) {
            clientBuilder.withCredentials(
               AWSStaticCredentialsProvider(
                  BasicAWSCredentials(
                     config.s3AccessKey, config.s3SecretKey)))
         }

         clientBuilder.withRegion(config.region)

         withExecutorFactory(object : ExecutorFactory {
            override fun newExecutor(): ExecutorService {
               return bufferExecutor
            }
         })

         withS3Client(clientBuilder.build())
      }.build()

      // Set credentials if necessary.
      if (config.awsAccessKey != null && config.awsAccessKey!!.isNotEmpty()) {
         builder.withCredentials(
            AWSStaticCredentialsProvider(
               BasicAWSCredentials(
                  config.awsAccessKey, config.awsSecretKey)))
      }

      builder.withClientConfiguration(ClientConfiguration()
         // Give a nice buffer to max connections based on max threads.
         .withMaxConnections(MAX_CONNECTIONS)
      )

      // Build SQS client and the Buffered client based on the "Real" SQS client.
      realSQS = builder.build()
      client = MoveAmazonSQSBufferedAsyncClient(realSQS)

      ActionManager.workerActionQueueGroupMap.forEach { entry ->
         val queueName = config.namespace + entry.key
         val baseQueueName = "action-worker" + entry.key
         LOG.info("Calling getQueueUrl() for " + queueName)

         val result = realSQS.getQueueUrl(queueName)

         val queueUrl =
            if (result == null || result.queueUrl == null || result.queueUrl.isEmpty()) {
               val createQueueResult = realSQS.createQueue(queueName)
               createQueueResult.queueUrl
            } else {
               result.queueUrl
            }

         // If there are more than 1 action mapped to this queue then find the max "parallelism"
         val maxParalellism = entry.value.map {
            it.parallelism()
         }.max()?.toInt() ?: DEFAULT_PARALLELISM
         // If there are more than 1 action mapped to this queue then find largest "timeoutMillis"
         val maxExecutionMillis = entry.value.map {
            it.timeoutMillis()
         }.max()?.toInt() ?: DEFAULT_WORKER_TIMEOUT_MILLIS
         // Determine if any action in the group is enabled as a worker.
         val workerEnabled = isWorkerEnabled && entry.value.find { workerEnabled(it) } != null

         val queueConfig = queueConfig(queueName)

         val bufferConfig = QueueBufferConfig()
            .withMaxBatchSize(queueConfig.maxBatchSize)
            .withMaxBatchOpenMs(queueConfig.maxBatchOpenMs.toLong())
            .withFlushOnShutdown(config.flushOnShutdown)

            // Turn off pre-fetching
            .withMaxInflightReceiveBatches(0)
            .withMaxDoneReceiveBatches(0)

         if (workerEnabled) {
            // Enable pre-fetching
            bufferConfig.withLongPoll(true)
            bufferConfig.withLongPollWaitTimeoutSeconds(20)
            bufferConfig.withMaxInflightReceiveBatches(queueConfig.maxInflightReceiveBatches)
            bufferConfig.withMaxDoneReceiveBatches(queueConfig.maxDoneReceiveBatches)

            if (queueConfig.maxInflightReceiveBatches > 0 && queueConfig.maxDoneReceiveBatches > 0) {
               bufferConfig.withVisibilityTimeoutSeconds(
                  TimeUnit.MILLISECONDS.toSeconds((maxExecutionMillis * VISIBILITY_MULTIPLIER).toLong()).toInt()
               )
            } else {
               bufferConfig.withVisibilityTimeoutSeconds(
                  TimeUnit.MILLISECONDS.toSeconds((maxExecutionMillis + VISIBILITY_PADDING_MILLIS)).toInt()
               )
            }
         }

         val buffer = client.putQBuffer(queueUrl, bufferConfig, bufferExecutor)

         val sender = SQSQueueSender(
            queueUrl,
            buffer
         )

         entry.value.forEach { ActionManager.bindProducer(it, sender) }

         val receiver: SQSQueueReceiver? = if (workerEnabled) {
            SQSQueueReceiver(
               vertx,
               queueName,
               baseQueueName,
               queueUrl,
               buffer,
               if (queueConfig.parallelism > 0)
                  queueConfig.parallelism
               else
                  0
            )
         } else {
            null
         }

         queueMap.put(
            queueName,
            QueueContext(sender, receiver, queueUrl, queueConfig, ImmutableSet.of(entry.value))
         )
      }

      // Startup all receivers.
      queueMap.values.forEach { queueContext ->
         if (queueContext.receiver != null) {
            try {
               queueContext.receiver.startAsync().awaitRunning()
            } catch (e: Throwable) {
               LOG.error("Failed to start SQSQueueReceiver for '" + queueContext.config.name + "'")
               throw RuntimeException(e)
            }

         }
      }

      timer = vertx.periodicStream(FILE_RETRY_AGE_SECONDS * 1000L).handler {
         cleanUpCaches()
      }
   }

   @Throws(Exception::class)
   override fun shutDown() {
      queueMap.values.forEach { queueContext ->
         if (queueContext.receiver != null) {
            queueContext.receiver.stopAsync().awaitTerminated()
         }
      }

      client.shutdown()
      s3TransferManager.shutdownNow(true)
      timer.cancel()
   }

   private fun cleanUpCaches() {
      vertx.rxExecuteBlocking<Unit> {
         try {
            activeFiles.cleanUp()
         } catch (e: Throwable) {
         }
         try {
            needToDeleteFiles.cleanUp()
         } catch (e: Throwable) {
         }
         try {
            activeUploads.cleanUp()
         } catch (e: Throwable) {
         }
         try {
            activeDownloads.cleanUp()
         } catch (e: Throwable) {
         }
      }
   }

   /**
    *
    */
   //   data class SQSEnvelope(val l: Boolean, val s: Int? = null, val name: String, val p: String)

   /**
    *
    */
   data class S3Pointer(val b: String, val k: String, val e: String)

   private inner class SQSQueueSender(
      private val queueUrl: String,
      private val buffer: QueueBuffer) : WorkerProducer {

      override fun send(request: WorkerRequest): Single<WorkerReceipt> {
         return Single.create<WorkerReceipt> { subscriber ->
            val actionProvider = if (request.actionProvider != null) {
               request.actionProvider
            } else {
               return@create
            }
            val workerAction: WorkerAction = if (actionProvider?.workerAction != null) {
               actionProvider.workerAction!!
            } else {
               return@create
            }

            val payload = WireFormat.byteify(request.request)

            if (payload.size > MAX_PAYLOAD_SIZE || workerAction.encrypted) {
               if (payload.size > config.maxPayloadSize) {
                  if (!subscriber.isUnsubscribed) {
                     subscriber.onError(RuntimeException("Payload of size '" + payload.size + "' exceeded max allowable '" + config.maxPayloadSize + "'"))
                  }
                  return@create
               }

               // Upload to S3.
               val id = UID.next()

               val putRequest = PutObjectRequest(
                  config.s3BucketName,
                  id,
                  ByteArrayInputStream(payload),
                  ObjectMetadata()
               )

               val upload = s3TransferManager.upload(putRequest)
               activeUploads.put(id, upload)
               val transfer = upload as AbstractTransfer
               transfer.addStateChangeListener { t, state ->
                  if (state == Transfer.TransferState.Completed) {
                     activeUploads.invalidate(id)
                     val uploadResult = upload.waitForUploadResult()

                     val sendRequest = SendMessageRequest(
                        queueUrl,
                        WorkerPacker.stringify(WorkerEnvelope(
                           1,
                           actionProvider.name,
                           payload.size,
                           WireFormat.byteify(S3Pointer(
                              uploadResult.bucketName,
                              uploadResult.key,
                              uploadResult.eTag)
                           )
                        ))
                     )

                     if (request.delaySeconds > 0) {
                        sendRequest.delaySeconds = request.delaySeconds
                     }

                     // Handle fifo
                     if (actionProvider.isFifo) {
                        var groupId = actionProvider.name

                        if (!request.groupId.isNullOrBlank()) {
                           groupId = request.groupId
                        }

                        sendRequest.messageGroupId = groupId
                        sendRequest.messageDeduplicationId = UID.next()
                     }

                     buffer.sendMessage(sendRequest, object : AsyncHandler<SendMessageRequest, SendMessageResult> {
                        override fun onError(exception: Exception) {
                           if (!subscriber.isUnsubscribed) {
                              subscriber.onError(exception)
                           }
                        }

                        override fun onSuccess(receiveRequest: SendMessageRequest, sendMessageResult: SendMessageResult?) {
                           if (!subscriber.isUnsubscribed) {
                              subscriber.onSuccess(WorkerReceipt().apply {
                                 messageId = sendMessageResult?.messageId
                                 mD5OfMessageBody = sendMessageResult?.mD5OfMessageBody
                                 mD5OfMessageAttributes = sendMessageResult?.mD5OfMessageAttributes
                                 sequenceNumber = sendMessageResult?.sequenceNumber
                              })
                           }
                        }
                     })
                  } else if (state == Transfer.TransferState.Failed || state == Transfer.TransferState.Canceled) {
                     activeUploads.invalidate(id)
                  }
               }
            } else {
               val sendRequest = SendMessageRequest(
                  queueUrl,
                  WorkerPacker.stringify(WorkerEnvelope(0, actionProvider.name, payload.size, payload))
               )

               if (request.delaySeconds > 0) {
                  sendRequest.delaySeconds = request.delaySeconds
               }

               // Handle fifo
               if (actionProvider.isFifo) {
                  var groupId = actionProvider.name

                  if (!request.groupId.isNullOrBlank()) {
                     groupId = request.groupId
                  }

                  sendRequest.messageGroupId = groupId
                  sendRequest.messageDeduplicationId = UID.next()
               }

               buffer.sendMessage(sendRequest, object : AsyncHandler<SendMessageRequest, SendMessageResult> {
                  override fun onError(exception: Exception) {
                     if (!subscriber.isUnsubscribed) {
                        subscriber.onError(exception)
                     }
                  }

                  override fun onSuccess(receiveRequest: SendMessageRequest, sendMessageResult: SendMessageResult?) {
                     if (!subscriber.isUnsubscribed) {
                        subscriber.onSuccess(WorkerReceipt().apply {
                           messageId = sendMessageResult?.messageId
                           mD5OfMessageBody = sendMessageResult?.mD5OfMessageBody
                           mD5OfMessageAttributes = sendMessageResult?.mD5OfMessageAttributes
                           sequenceNumber = sendMessageResult?.sequenceNumber
                        })
                     }
                  }
               })
            }
         }
      }
   }

   /**
    * Processes Worker requests from a single AmazonSQS Queue.
    * Supports parallel receive and delete threads and utilizes SQS batching to increase message throughput.
    *
    *
    * Reliable
    */
   private inner class SQSQueueReceiver(val vertx: Vertx,
                                        val queueName: String,
                                        val baseQueueName: String,
                                        val queueUrl: String,
                                        val sqsBuffer: QueueBuffer,
                                        parallelism: Int) : AbstractIdleService() {
      val registry = Metrics.registry()

       private val secondsSinceLastPollGauge: Gauge<Long> = try {
           registry.register<Gauge<Long>>(
                   baseQueueName + "-SECONDS_SINCE_LAST_POLL",
                   Gauge<Long> { TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastPoll) }
           )
       } catch (e: Throwable) {
           registry.metrics[baseQueueName + "-SECONDS_SINCE_LAST_POLL"] as Gauge<Long>
       }

       private val activeMessagesGauge: Gauge<Int> = try {
           registry.register<Gauge<Int>>(
                   baseQueueName + "-ACTIVE_MESSAGES",
                   Gauge<Int> { activeMessages.get() }
           )
       } catch (e: Throwable) {
           registry.metrics[baseQueueName + "-ACTIVE_MESSAGES"] as Gauge<Int>
       }
       private val parallelismGauge: Gauge<Int> = try {
           registry.register<Gauge<Int>>(
                   baseQueueName + "-PARALLELISM",
                   Gauge<Int> { concurrentRequests }
           )
       } catch (e: Throwable) {
           registry.metrics[baseQueueName + "-PARALLELISM"] as Gauge<Int>
       }
       private val jobsCounter: Counter = registry.counter(baseQueueName + "-JOBS")
       private val timeoutsCounter: Counter = registry.counter(baseQueueName + "-TIMEOUTS")
       private val completesCounter: Counter = registry.counter(baseQueueName + "-COMPLETES")
       private val inCompletesCounter: Counter = registry.counter(baseQueueName + "-IN_COMPLETES")
       private val exceptionsCounter: Counter = registry.counter(baseQueueName + "-EXCEPTIONS")
       private val deletesCounter: Counter = registry.counter(baseQueueName + "-DELETES")
       private val deleteFailuresCounter: Counter = registry.counter(baseQueueName + "-DELETE_FAILURES")
       private val receiveLatencyCounter: Counter = registry.counter(baseQueueName + "-RECEIVE_LATENCY")
       private val noMessagesCounter: Counter = registry.counter(baseQueueName + "-NO_MESSAGES")
       private val downloadsCounter: Counter = registry.counter(baseQueueName + "-DOWNLOADS")
       private val downloadsNanosCounter: Counter = registry.counter(baseQueueName + "-DOWNLOAD_NANOS")
       private val downloadedBytesCounter: Counter = registry.counter(baseQueueName + "-DOWNLOADED_BYTES")

      @Volatile private var lastPoll: Long = 0

      private val activeMessages = AtomicInteger(0)

      var concurrentRequests = if (parallelism < MIN_RECEIVE_THREADS) {
         MIN_RECEIVE_THREADS
      } else if (parallelism > MAX_RECEIVE_THREADS) {
         MAX_RECEIVE_THREADS
      } else {
         parallelism
      }

      @Throws(Exception::class)
      override fun startUp() {
//            LOG.debug("Starting up SQS service.")
         LOG.info("SQS Queue: ")
         receiveMore()
      }

      @Throws(Exception::class)
      override fun shutDown() {
         sqsBuffer.shutdown()
      }

      fun shouldRun(): Boolean = when (state()) {
         Service.State.STARTING, Service.State.NEW, Service.State.RUNNING -> true
         else -> false
      }

      fun deleteMessage(receiptHandle: String) {
         // Delete asynchronously.
         // Allow deletes to be batched which can increase performance quite a bit while decreasing
         // resources used and allowing allowing the next receive to happen immediately.
         // The visibility timeout buffer provides a good safety net.
         sqsBuffer.deleteMessage(DeleteMessageRequest(queueUrl, receiptHandle), object : AsyncHandler<DeleteMessageRequest, DeleteMessageResult> {
            override fun onError(exception: Exception) {
               receiveMore()
               LOG.error("Failed to delete message for receipt handle " + receiptHandle, exception)
               deleteFailuresCounter.inc()
            }

            override fun onSuccess(receiveRequest: DeleteMessageRequest, deleteMessageResult: DeleteMessageResult) {
               // Ignore.
               receiveMore()
            }
         })
      }

      @Synchronized
      fun receiveMore() {
         if (!shouldRun())
            return

         if (activeMessages.get() >= concurrentRequests)
            return

         val request = ReceiveMessageRequest(queueUrl)
         // SQS max number of messages is capped at 10.
         request.maxNumberOfMessages = Math.min(MAX_NUMBER_OF_MESSAGES, concurrentRequests - activeMessages.get())
         // Give it the max 20 second poll. This will make it as cheap as possible.
         request.waitTimeSeconds = SQS_POLL_WAIT_SECONDS

         if (request.maxNumberOfMessages <= 0) {
            return
         }

         lastPoll = Math.max(System.currentTimeMillis(), lastPoll)
         val start = System.currentTimeMillis()
         activeMessages.addAndGet(request.maxNumberOfMessages)

         sqsBuffer.receiveMessage(request, object : AsyncHandler<ReceiveMessageRequest, ReceiveMessageResult> {
            override fun onError(exception: java.lang.Exception?) {
               try {
                  activeMessages.addAndGet(-request.maxNumberOfMessages)
                  receiveLatencyCounter.inc(System.currentTimeMillis() - start)
                  LOG.error("ReceiveMessage for " + queueName + " threw an exception", exception)
               } finally {
                  receiveMore()
               }
            }

            override fun onSuccess(receiveRequest: ReceiveMessageRequest?, result: ReceiveMessageResult?) {
               receiveLatencyCounter.inc(System.currentTimeMillis() - start)

               // Were there any messages?
               if (result == null || result.messages == null || result.messages.isEmpty()) {
                  activeMessages.addAndGet(-request.maxNumberOfMessages)
                  noMessagesCounter.inc()
                  receiveMore()
                  return
               }

               try {
                  activeMessages.addAndGet(result.messages.size - request.maxNumberOfMessages)

                  result.messages.forEach { message ->
                     // Parse request.
                     val envelope = WorkerPacker.parse(Strings.nullToEmpty(message.body))

                     if (envelope.option == 1) {
                        val s3Pointer = WireFormat.parse(S3Pointer::class.java, envelope.body)

                        if (s3Pointer == null) {
                           LOG.error("Invalid S3Pointer json: " + envelope.body)
                           deleteMessage(message.receiptHandle)
                        } else {
                           downloadsCounter.inc()
                           val downloadStarted = System.nanoTime()

                           vertx.rxExecuteBlocking<Unit> {
                              // Create a temp file and let's keep track of it.
                              val file = File.createTempFile("move_sqs_" + s3Pointer.k, "json")
                              activeFiles.put(s3Pointer.k, file)

                              // Start S3 Download.
                              val download = s3TransferManager.download(s3Pointer.b, s3Pointer.k, file)
                              activeDownloads.put(s3Pointer.k, download)

                              (download as AbstractTransfer).addStateChangeListener { transfer, state ->
                                 if (state == Transfer.TransferState.Completed) {
                                    activeDownloads.invalidate(s3Pointer.k)
                                    val length = file.length()
                                    downloadedBytesCounter.inc(length)
                                    downloadsNanosCounter.inc(System.nanoTime() - downloadStarted)

                                    if (length > config.maxPayloadSize) {
                                       if (!file.delete()) {
                                          needToDeleteFiles.put(s3Pointer.k, file)
                                       }
                                       activeFiles.invalidate(s3Pointer.k)

                                       LOG.error("Payload of size '" + length + "' exceeded max allowable '" + config.maxPayloadSize + "'")
                                       return@addStateChangeListener
                                    }

                                    if (isRunning) {
                                       val ctx = Vertx.currentContext()
                                       if (ctx != null && ctx.isEventLoopContext) {
                                          fileSystem.rxReadFile(file.absolutePath).subscribe(
                                             {
                                                if (!file.delete()) {
                                                   needToDeleteFiles.put(s3Pointer.k, file)
                                                }
                                                activeFiles.invalidate(s3Pointer.k)

                                                call(message, envelope, it.delegate.bytes)
                                             },
                                             {
                                                if (!file.delete()) {
                                                   needToDeleteFiles.put(s3Pointer.k, file)
                                                }
                                                activeFiles.invalidate(s3Pointer.k)
                                                LOG.error("Failed to read File '" + file.absoluteFile + "'", it)
                                             }
                                          )
                                       } else {
                                          val body = try {
                                             Files.readAllBytes(file.toPath())
                                          } catch (e: Throwable) {
                                             LOG.error("Failed to read file '" + file.absolutePath + "'", e)
                                             null
                                          }

                                          if (!file.delete()) {
                                             needToDeleteFiles.put(s3Pointer.k, file)
                                          }
                                          activeFiles.invalidate(s3Pointer.k)

                                          if (body == null) {
                                             receiveMore()
                                          } else {
                                             vertx.runOnContext {
                                                call(message, envelope, body)
                                             }
                                          }
                                       }
                                    }
                                 } else if (state == Transfer.TransferState.Failed || state == Transfer.TransferState.Canceled) {
                                    activeDownloads.invalidate(s3Pointer.k)
                                    LOG.warn("Failed to Download payload from S3 for message handle '" + message.receiptHandle + "'. Reason '" + state.name + "'")
                                 }
                              }
                           }.subscribe(
                              {
                              },
                              {
                                 LOG.error("Failed to start Download", it)
                              }
                           )
                        }
                     } else {
                        call(message, envelope, envelope.body)
                     }
                  }
               } finally {
                  receiveMore()
               }
            }
         })

         // Try to receive more.
         receiveMore()
      }

      fun call(message: Message, envelope: WorkerEnvelope, body: ByteArray) {
         if (!shouldRun()) {
            activeMessages.decrementAndGet()
            return
         }

         val actionProvider = ActionManager.workerActionMap[envelope.name]
         if (actionProvider == null) {
            LOG.error("Could not find action named '" + envelope.name + "'. Deleting...")
            deletesCounter.inc()
            activeMessages.decrementAndGet()
            deleteMessage(message.receiptHandle)
            receiveMore()
            return
         }

         try {
            val request: Any = if (body.isEmpty() || (body.size == 2 && body[0] == '{'.toByte() && body[1] == '}'.toByte()))
               actionProvider.inProvider.get()
            else
               WireFormat.parse(
                  actionProvider.inClass,
                  body
               ) ?: actionProvider.inProvider.get()

            jobsCounter.inc()

            actionProvider.local(request).subscribe(
               {
                  activeMessages.decrementAndGet()
                  Try.run { receiveMore() }

                  if (it != null && it) {
                     completesCounter.inc()
                     deletesCounter.inc()

                     try {
                        deleteMessage(message.receiptHandle)
                     } catch (e: Throwable) {
                        LOG.error("Delete message from queue '" + queueUrl + "' with receipt handle '" + message.receiptHandle + "' failed", e)
                        deleteFailuresCounter.inc()
                     }
                  } else {
                     inCompletesCounter.inc()
                  }
               },
               {
                  activeMessages.decrementAndGet()
                  Try.run { receiveMore() }
                  LOG.error("Action " + actionProvider.actionClass.canonicalName + " threw an exception", it)

                  val rootCause = Throwables.getRootCause(it)
                  if (rootCause is HystrixTimeoutException || rootCause is TimeoutException) {
                     timeoutsCounter.inc()
                  } else {
                     exceptionsCounter.inc()
                  }
               }
            )
         } catch (e: Exception) {
            activeMessages.decrementAndGet()
            Try.run { receiveMore() }

            LOG.error("Action " + actionProvider.actionClass.canonicalName + " threw an exception", e)

            val rootCause = Throwables.getRootCause(e)
            if (rootCause is HystrixTimeoutException || rootCause is TimeoutException) {
               timeoutsCounter.inc()
            } else {
               exceptionsCounter.inc()
            }
         }
      }

      private inner class InternalInterruptedException(cause: Throwable) : RuntimeException(cause)
   }

   /**

    */
   private inner class QueueContext(val sender: SQSQueueSender,
                                    val receiver: SQSQueueReceiver?,
                                    val queueUrl: String,
                                    val config: SQSQueueConfig,
                                    var actionProviders: ImmutableSet<Set<WorkerActionProvider<Action<Any, Boolean>, Any>>>)

   class QueueConfiguration {
      var name: String? = null
      var config: SQSQueueConfig? = null
      var actions: List<String>? = null
   }

   companion object {
      private val LOG = LoggerFactory.getLogger(SQSService::class.java)

      private val MIN_THREADS = 1
      private val MAX_THREADS = 10000
      private val THREADS_MULTIPLIER = 2.0
      private val MAX_CONNECTIONS = 100
      private val MAX_NUMBER_OF_MESSAGES = 10
      // Default to 2 hours.
      private val VISIBILITY_MULTIPLIER = 2.1
      private val VISIBILITY_PADDING_MILLIS = 2_000L
      private val FILE_RETRY_AGE_SECONDS = 10L
      private val MAX_CACHE_AGE_MINUTES = 120L
      private val MAX_FILE_CACHE = 100_000L
      private val MAX_UPLOADS = 100_000L
      private val MAX_DOWNLOADS = 100_000L
      private val SQS_POLL_WAIT_SECONDS = 20 // Can be a value between 1 and 20
      private val KEEP_ALIVE_SECONDS = 60L
      private val MAX_PAYLOAD_SIZE = 255000
      private val MIN_RECEIVE_THREADS = 1
      private val MAX_RECEIVE_THREADS = 1024
      private val DEFAULT_PARALLELISM = Runtime.getRuntime().availableProcessors() * 2
      private val DEFAULT_WORKER_TIMEOUT_MILLIS = 10000

      private val COMPRESSION_MIN = 500

//      private val NULL_ENVELOPE = SQSEnvelope(false, 0, "", "")
//
//      fun stringify(e: SQSEnvelope): String {
//         val builder = StringBuilder()
//
//         if (e.p.length < COMPRESSION_MIN) {
//            builder.append("-")
//         }
//
//         if (e.l)
//            builder.append("1")
//         else
//            builder.append("0")
//
//         builder.append(e.s).append("|").append(e.name).append("|").append(e.p)
//
//         if (e.p.length < COMPRESSION_MIN) {
//            return builder.toString()
//         }
//
//         return GZIPCompression.pack(builder.toString())
//      }
//
//      fun parse(packed: String): SQSEnvelope {
//         if (packed.isNullOrBlank()) {
//            return NULL_ENVELOPE
//         }
//
//         val p = if (packed[0] == '-') {
//            packed.substring(1)
//         } else {
//            GZIPCompression.unpack(packed)
//         }
//
//         val large = p[0] == '1'
//
//         var indexOfBar = p.indexOf('|')
//         if (indexOfBar < 2) {
//            return NULL_ENVELOPE
//         }
//
//         val sizeSubstr = p.substring(1, indexOfBar)
//         val size = try {
//            Integer.parseInt(sizeSubstr)
//         } catch (e: Exception) {
//            0
//         }
//
//         val remaining = p.substring(indexOfBar + 1)
//
//         indexOfBar = remaining.indexOf('|')
//
//         val name = if (indexOfBar <= 0) "" else try {
//            remaining.substring(0, indexOfBar)
//         } catch (e: Exception) {
//            return NULL_ENVELOPE
//         }
//
//         val payload = if (indexOfBar <= 0 || indexOfBar + 1 == remaining.length) "" else try {
//            remaining.substring(indexOfBar + 1)
//         } catch (e: Exception) {
//            ""
//         }
//
//         return SQSEnvelope(large, size, name, payload)
//      }
//
//      @JvmStatic
//      fun main(args: Array<String>) {
//
//         val payload = "{\"doc\":{\"orgId\":\"a1acf243e8dc44f3bbb734327b40cd04\",\"orgUnitId\":\"ab6542f861b7476cae96ef65f19532f7\",\"generatedByUserId\":\"20133faf53554c2eb6cf21551e7517ff\",\"docReportType\":\"SALES_ORDER_PO_REQUEST\",\"format\":\"PDF\",\"displayType\":\"WEB\",\"requestClassName\":\"action.worker.docreport.order.GenerateSalesOrderPORequest\",\"parameters\":\"{\\\"orderId\\\":\\\"544c71a4631a430582f56c54aa8def0e\\\"}\",\"startDate\":\"2017-08-08T19:43:01.109Z\",\"endDate\":null,\"processingTimeSeconds\":0.0,\"expiresOnDate\":\"2017-08-09T19:43:01.109Z\",\"status\":\"PENDING\",\"timeout\":\"2017-08-08T20:43:01.109Z\",\"attempt\":1,\"maxDownloads\":5,\"v\":0,\"id\":\"9e859f44280a4501a15126d08fffe915\"},\"orderId\":\"544c71a4631a430582f56c54aa8def0e\"}"
////         val payload = "{\"doc\":{\"orgId\":\"a1acf243e8dc44f3bbb734327b40cd04\",\"orgUnitId\":\"ab6542f861b7476cae96ef65f19532f7\",\"generatedByUserId\":\"20133faf53554c2eb6cf21551e7517ff\",\"docReportType\":\"SALES_ORDER_PO_REQUEST\",\"format\":\"PDF\",\"displayType\":\"WEB\",\"requestClassName\":\"action.worker.docreport.order.GenerateSalesOrderPORequest\",\"parameters\":\"{\\\"orderId\\\":\\\"544c71a4631a430582f56c54aa8def0e\\\"}\",\"startDate\":\"2017-08-08T19:43:01.109Z\",\"endDate\":null,\"processingTimeSeconds\":0.0,\"expiresOnDate\":\"2017-08-09T19:43:01.109Z\",\"status\":\"PENDING\",\"timeout\":\"2017-08-08T20:43:01.109Z\",\"attempt\":1,\"maxDownloads\":5,\"v\":0,\"id\":\"9e859f44280a4501a15126d08fffe915\"},\"orderId\":\"544c71a4631a430582f56c54aa8def0e\"}/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/bin/java -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:51853,suspend=y,server=n -Dfile.encoding=UTF-8 -classpath \"/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/charsets.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/deploy.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/ext/cldrdata.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/ext/dnsns.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/ext/jaccess.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/ext/jfxrt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/ext/localedata.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/ext/nashorn.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/ext/sunec.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/ext/sunjce_provider.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/ext/sunpkcs11.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/ext/zipfs.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/javaws.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/jce.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/jfr.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/jfxswt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/jsse.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/management-agent.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/plugin.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/resources.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/rt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/lib/ant-javafx.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/lib/dt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/lib/javafx-mx.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/lib/jconsole.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/lib/packager.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/lib/sa-jdi.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/lib/tools.jar:/Users/clay/repos/move/cloud/action-sqs/target/classes:/Users/clay/.m2/repository/com/google/dagger/dagger/2.11/dagger-2.11.jar:/Users/clay/.m2/repository/javax/inject/javax.inject/1/javax.inject-1.jar:/Users/clay/.m2/repository/org/jetbrains/kotlin/kotlin-stdlib-jre8/1.1.3-2/kotlin-stdlib-jre8-1.1.3-2.jar:/Users/clay/.m2/repository/org/jetbrains/kotlin/kotlin-stdlib/1.1.3-2/kotlin-stdlib-1.1.3-2.jar:/Users/clay/.m2/repository/org/jetbrains/annotations/13.0/annotations-13.0.jar:/Users/clay/.m2/repository/org/jetbrains/kotlin/kotlin-stdlib-jre7/1.1.3-2/kotlin-stdlib-jre7-1.1.3-2.jar:/Users/clay/repos/move/cloud/action/target/classes:/Users/clay/repos/move/cloud/common/target/classes:/Users/clay/.m2/repository/org/jetbrains/kotlinx/kotlinx-coroutines-core/0.16/kotlinx-coroutines-core-0.16.jar:/Users/clay/.m2/repository/org/jetbrains/kotlinx/kotlinx-coroutines-rx1/0.16/kotlinx-coroutines-rx1-0.16.jar:/Users/clay/.m2/repository/io/javaslang/javaslang/2.0.4/javaslang-2.0.4.jar:/Users/clay/.m2/repository/io/javaslang/javaslang-match/2.0.4/javaslang-match-2.0.4.jar:/Users/clay/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.8.9/jackson-core-2.8.9.jar:/Users/clay/.m2/repository/com/fasterxml/jackson/core/jackson-databind/2.8.9/jackson-databind-2.8.9.jar:/Users/clay/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.8.9/jackson-annotations-2.8.9.jar:/Users/clay/.m2/repository/com/fasterxml/jackson/datatype/jackson-datatype-jsr310/2.8.9/jackson-datatype-jsr310-2.8.9.jar:/Users/clay/.m2/repository/com/fasterxml/jackson/datatype/jackson-datatype-jdk8/2.8.9/jackson-datatype-jdk8-2.8.9.jar:/Users/clay/.m2/repository/com/fasterxml/jackson/datatype/jackson-datatype-guava/2.8.9/jackson-datatype-guava-2.8.9.jar:/Users/clay/.m2/repository/com/hazelcast/hazelcast-all/3.8.3/hazelcast-all-3.8.3.jar:/Users/clay/.m2/repository/io/vertx/vertx-core/3.4.2/vertx-core-3.4.2.jar:/Users/clay/.m2/repository/io/netty/netty-common/4.1.8.Final/netty-common-4.1.8.Final.jar:/Users/clay/.m2/repository/io/netty/netty-buffer/4.1.8.Final/netty-buffer-4.1.8.Final.jar:/Users/clay/.m2/repository/io/netty/netty-transport/4.1.8.Final/netty-transport-4.1.8.Final.jar:/Users/clay/.m2/repository/io/netty/netty-handler/4.1.8.Final/netty-handler-4.1.8.Final.jar:/Users/clay/.m2/repository/io/netty/netty-codec/4.1.8.Final/netty-codec-4.1.8.Final.jar:/Users/clay/.m2/repository/io/netty/netty-handler-proxy/4.1.8.Final/netty-handler-proxy-4.1.8.Final.jar:/Users/clay/.m2/repository/io/netty/netty-codec-socks/4.1.8.Final/netty-codec-socks-4.1.8.Final.jar:/Users/clay/.m2/repository/io/netty/netty-codec-http/4.1.8.Final/netty-codec-http-4.1.8.Final.jar:/Users/clay/.m2/repository/io/netty/netty-codec-http2/4.1.8.Final/netty-codec-http2-4.1.8.Final.jar:/Users/clay/.m2/repository/io/netty/netty-resolver/4.1.8.Final/netty-resolver-4.1.8.Final.jar:/Users/clay/.m2/repository/io/netty/netty-resolver-dns/4.1.8.Final/netty-resolver-dns-4.1.8.Final.jar:/Users/clay/.m2/repository/io/netty/netty-codec-dns/4.1.8.Final/netty-codec-dns-4.1.8.Final.jar:/Users/clay/.m2/repository/io/vertx/vertx-web/3.4.2/vertx-web-3.4.2.jar:/Users/clay/.m2/repository/io/vertx/vertx-auth-common/3.4.2/vertx-auth-common-3.4.2.jar:/Users/clay/.m2/repository/io/vertx/vertx-rx-java/3.4.2/vertx-rx-java-3.4.2.jar:/Users/clay/.m2/repository/io/vertx/vertx-hazelcast/3.4.2/vertx-hazelcast-3.4.2.jar:/Users/clay/.m2/repository/io/vertx/vertx-dropwizard-metrics/3.4.2/vertx-dropwizard-metrics-3.4.2.jar:/Users/clay/.m2/repository/com/google/guava/guava/21.0/guava-21.0.jar:/Users/clay/.m2/repository/org/reflections/reflections/0.9.11/reflections-0.9.11.jar:/Users/clay/.m2/repository/org/javassist/javassist/3.21.0-GA/javassist-3.21.0-GA.jar:/Users/clay/.m2/repository/javax/validation/validation-api/1.1.0.Final/validation-api-1.1.0.Final.jar:/Users/clay/.m2/repository/joda-time/joda-time/2.9.4/joda-time-2.9.4.jar:/Users/clay/.m2/repository/io/dropwizard/metrics/metrics-core/3.2.3/metrics-core-3.2.3.jar:/Users/clay/.m2/repository/io/dropwizard/metrics/metrics-healthchecks/3.2.3/metrics-healthchecks-3.2.3.jar:/Users/clay/.m2/repository/io/dropwizard/metrics/metrics-graphite/3.2.3/metrics-graphite-3.2.3.jar:/Users/clay/.m2/repository/io/vertx/vertx-circuit-breaker/3.4.2/vertx-circuit-breaker-3.4.2.jar:/Users/clay/.m2/repository/com/netflix/hystrix/hystrix-core/1.5.12/hystrix-core-1.5.12.jar:/Users/clay/.m2/repository/org/slf4j/slf4j-api/1.7.24/slf4j-api-1.7.24.jar:/Users/clay/.m2/repository/com/netflix/archaius/archaius-core/0.4.1/archaius-core-0.4.1.jar:/Users/clay/.m2/repository/commons-configuration/commons-configuration/1.8/commons-configuration-1.8.jar:/Users/clay/.m2/repository/commons-lang/commons-lang/2.6/commons-lang-2.6.jar:/Users/clay/.m2/repository/io/reactivex/rxjava/1.3.0/rxjava-1.3.0.jar:/Users/clay/.m2/repository/org/hdrhistogram/HdrHistogram/2.1.9/HdrHistogram-2.1.9.jar:/Users/clay/.m2/repository/com/amazonaws/aws-java-sdk-sqs/1.11.166/aws-java-sdk-sqs-1.11.166.jar:/Users/clay/.m2/repository/com/amazonaws/aws-java-sdk-core/1.11.166/aws-java-sdk-core-1.11.166.jar:/Users/clay/.m2/repository/commons-logging/commons-logging/1.1.3/commons-logging-1.1.3.jar:/Users/clay/.m2/repository/org/apache/httpcomponents/httpclient/4.5.2/httpclient-4.5.2.jar:/Users/clay/.m2/repository/org/apache/httpcomponents/httpcore/4.4.4/httpcore-4.4.4.jar:/Users/clay/.m2/repository/commons-codec/commons-codec/1.9/commons-codec-1.9.jar:/Users/clay/.m2/repository/software/amazon/ion/ion-java/1.0.2/ion-java-1.0.2.jar:/Users/clay/.m2/repository/com/fasterxml/jackson/dataformat/jackson-dataformat-cbor/2.6.7/jackson-dataformat-cbor-2.6.7.jar:/Users/clay/.m2/repository/com/amazonaws/jmespath-java/1.11.166/jmespath-java-1.11.166.jar:/Users/clay/.m2/repository/com/amazonaws/aws-java-sdk-s3/1.11.166/aws-java-sdk-s3-1.11.166.jar:/Users/clay/.m2/repository/com/amazonaws/aws-java-sdk-kms/1.11.166/aws-java-sdk-kms-1.11.166.jar:/Users/clay/.m2/repository/com/squareup/javapoet/1.9.0/javapoet-1.9.0.jar:/Applications/IntelliJ IDEA.app/Contents/lib/idea_rt.jar\" move.action.SQSService\n" +
////            "Connected to the target VM, address: '127.0.0.1:51853', transport: 'socket'\n" +
////            "SLF4J: Failed to load class \"org.slf4j.impl.StaticLoggerBinder\".\n" +
////            "SLF4J: Defaulting to no-operation (NOP) logger implementation\n" +
////            "SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.\n" +
////            "Raw: 692   Packed: 648   FastLZ: 587    FastLZ-Packed: 784      GZIP: 462\n" +
////            "Disconnected from the target VM, address: '127.0.0.1:51853', transport: 'socket'\n" +
////            "01000|move.MyAction|{}\n" +
////            "11000|move.MyAction|{}\n" +
////            "SQSEnvelope(l=false, s=1000, name=move.MyAction, p={})\n" +
////            "\n" +
////            "Process finished with exit code 0"
//         val packed = GZIPCompression.pack(payload)
//         val compressed = FastLZ.compress(payload)
//         val gzipCompressed = GZIPCompression.compress(payload)
//         val lzPacked = BaseEncoding.base64().encode(compressed)
//         println("Raw: " + payload.length + "   Packed: " + packed.length + "   FastLZ: " + compressed.size + "    FastLZ-Packed: " + lzPacked.length + "      GZIP: " + gzipCompressed.size)
//         val unpacked = GZIPCompression.unpack(packed)
//
//         val envelopeSerialized = stringify(SQSEnvelope(false, 1000, "move.MyAction", "{}"))
//         println(stringify(SQSEnvelope(false, 1000, "move.MyAction", "{}")))
//         println(stringify(SQSEnvelope(true, 1000, "move.MyAction", "{}")))
//
//         val envelope = parse(envelopeSerialized)
//
//         println(envelope)
//      }
   }
}
