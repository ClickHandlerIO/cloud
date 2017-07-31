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
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.AbstractIdleService
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
import java.io.File
import java.nio.charset.StandardCharsets
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
            QueueContext(sender, receiver, queueUrl, queueConfig, ImmutableList.of(entry.value))
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
   data class SQSEnvelope(val l: Boolean, val s: Int? = null, val name: String, val p: String)

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

            val payload = WireFormat.stringify(request.request)

            if (payload.length > MAX_PAYLOAD_SIZE || workerAction.encrypted) {
               if (payload.length > config.maxPayloadSize) {
                  if (!subscriber.isUnsubscribed) {
                     subscriber.onError(RuntimeException("Payload of size '" + payload.length + "' exceeded max allowable '" + config.maxPayloadSize + "'"))
                  }
                  return@create
               }

               // Upload to S3.
               val id = UID.next()

               val putRequest = PutObjectRequest(
                  config.s3BucketName,
                  id,
                  payload.byteInputStream(Charsets.UTF_8),
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
                        stringify(SQSEnvelope(
                           true,
                           payload.length,
                           actionProvider.name,
                           WireFormat.stringify(S3Pointer(
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
                  stringify(SQSEnvelope(false, payload.length, actionProvider.name, payload))
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
                                        val queueUrl: String,
                                        val sqsBuffer: QueueBuffer,
                                        parallelism: Int) : AbstractIdleService() {
      val registry = Metrics.registry()

      private val secondsSinceLastPollGauge: Gauge<Long> = try {
         registry.register<Gauge<Long>>(
            queueName + "-SECONDS_SINCE_LAST_POLL",
            Gauge<Long> { TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastPoll) }
         )
      } catch (e: Throwable) {
         registry.metrics[queueName + "-SECONDS_SINCE_LAST_POLL"] as Gauge<Long>
      }

      private val activeMessagesGauge: Gauge<Int> = try {
         registry.register<Gauge<Int>>(
            queueName + "-ACTIVE_MESSAGES",
            Gauge<Int> { activeMessages.get() }
         )
      } catch (e: Throwable) {
         registry.metrics[queueName + "-ACTIVE_MESSAGES"] as Gauge<Int>
      }
      private val parallelismGauge: Gauge<Int> = try {
         registry.register<Gauge<Int>>(
            queueName + "-PARALLELISM",
            Gauge<Int> { concurrentRequests }
         )
      } catch (e: Throwable) {
         registry.metrics[queueName + "-PARALLELISM"] as Gauge<Int>
      }
      private val jobsCounter: Counter = registry.counter(queueName + "-JOBS")
      private val timeoutsCounter: Counter = registry.counter(queueName + "-TIMEOUTS")
      private val completesCounter: Counter = registry.counter(queueName + "-COMPLETES")
      private val inCompletesCounter: Counter = registry.counter(queueName + "-IN_COMPLETES")
      private val exceptionsCounter: Counter = registry.counter(queueName + "-EXCEPTIONS")
      private val deletesCounter: Counter = registry.counter(queueName + "-DELETES")
      private val deleteFailuresCounter: Counter = registry.counter(queueName + "-DELETE_FAILURES")
      private val receiveLatencyCounter: Counter = registry.counter(queueName + "-RECEIVE_LATENCY")
      private val noMessagesCounter: Counter = registry.counter(queueName + "-NO_MESSAGES")
      private val downloadsCounter: Counter = registry.counter(queueName + "-DOWNLOADS")
      private val downloadsNanosCounter: Counter = registry.counter(queueName + "-DOWNLOAD_NANOS")
      private val downloadedBytesCounter: Counter = registry.counter(queueName + "-DOWNLOADED_BYTES")

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
         receiveMore()
      }

      @Throws(Exception::class)
      override fun shutDown() {
         sqsBuffer.shutdown()
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

      fun receiveMore() {
         if (!isRunning)
            return

         if (activeMessages.get() >= concurrentRequests)
            return

         val request = ReceiveMessageRequest(queueUrl)
         // SQS max number of messages is capped at 10.
         request.maxNumberOfMessages = Math.min(10, concurrentRequests - activeMessages.get())
         // Give it the max 20 second poll. This will make it as cheap as possible.
         request.waitTimeSeconds = SQS_POLL_WAIT_SECONDS

         if (request.maxNumberOfMessages <= 0)
            return

         lastPoll = Math.max(System.currentTimeMillis(), lastPoll)
         val start = System.currentTimeMillis()

         sqsBuffer.receiveMessage(request, object : AsyncHandler<ReceiveMessageRequest, ReceiveMessageResult> {
            override fun onError(exception: java.lang.Exception?) {
               receiveLatencyCounter.inc(System.currentTimeMillis() - start)
               LOG.error("ReceiveMessage for " + queueName + " threw an exception", exception)
               receiveMore()
            }

            override fun onSuccess(receiveRequest: ReceiveMessageRequest?, result: ReceiveMessageResult?) {
               receiveLatencyCounter.inc(System.currentTimeMillis() - start)

               // Were there any messages?
               if (result == null || result.messages == null || result.messages.isEmpty()) {
                  noMessagesCounter.inc()
                  receiveMore()
                  return
               }

               activeMessages.addAndGet(result.messages.size)

               result.messages.forEach { message ->
                  // Parse request.
                  val envelope = parse(Strings.nullToEmpty(message.body))

                  if (envelope.l) {
                     val s3Pointer = WireFormat.parse(S3Pointer::class.java, envelope.p)

                     if (s3Pointer == null) {
                        LOG.error("Invalid S3Pointer json: " + envelope.p)
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

                                             call(message, envelope, it.toString())
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
                                          val payload = if (body.isEmpty())
                                             ""
                                          else
                                             String(body, StandardCharsets.UTF_8)

                                          vertx.runOnContext {
                                             call(message, envelope, payload)
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
                     call(message, envelope, envelope.p)
                  }
               }
            }
         })

         // Try to receive more.
         receiveMore()
      }

      fun call(message: Message, envelope: SQSEnvelope, body: String) {
         if (!isRunning)
            return

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
            val request: Any = if (body.isEmpty() || "{}" == body)
               actionProvider.inProvider.get()
            else
               WireFormat.parse(
                  actionProvider.inClass,
                  body
               ) ?: actionProvider.inProvider.get()

            jobsCounter.inc()

            actionProvider.single(request).subscribe(
               {
                  activeMessages.decrementAndGet()

                  if (it != null && it.isSuccess) {
                     completesCounter.inc()
                     deletesCounter.inc()
                     receiveMore()

                     try {
                        deleteMessage(message.receiptHandle)
                     } catch (e: Throwable) {
                        LOG.error("Delete message from queue '" + queueUrl + "' with receipt handle '" + message.receiptHandle + "' failed", e)
                        deleteFailuresCounter.inc()
                        receiveMore()
                     }
                  } else {
                     inCompletesCounter.inc()
                     receiveMore()
                  }
               },
               {
                  activeMessages.decrementAndGet()
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
            receiveMore()
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
                                    var actionProviders: ImmutableList<List<WorkerActionProvider<Action<Any, Boolean>, Any>>>)

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

      private val NULL_ENVELOPE = SQSEnvelope(false, 0, "", "")

      fun stringify(e: SQSEnvelope): String {
         val builder = StringBuilder()

         if (e.l)
            builder.append("1")
         else
            builder.append("0")

         return builder.append(e.s).append("|").append(e.name).append("|").append(e.p).toString()
      }

      fun parse(p: String): SQSEnvelope {
         if (p.isNullOrBlank()) {
            return NULL_ENVELOPE
         }

         val large = p[0] == '1'

         var indexOfBar = p.indexOf('|')
         if (indexOfBar < 2) {
            return NULL_ENVELOPE
         }

         val sizeSubstr = p.substring(1, indexOfBar)
         val size = try {
            Integer.parseInt(sizeSubstr)
         } catch (e: Exception) {
            0
         }

         val remaining = p.substring(indexOfBar + 1)

         indexOfBar = remaining.indexOf('|')

         val name = if (indexOfBar <= 0) "" else try {
            remaining.substring(0, indexOfBar)
         } catch (e: Exception) {
            return NULL_ENVELOPE
         }

         val payload = if (indexOfBar <= 0 || indexOfBar + 1 == remaining.length) "" else try {
            remaining.substring(indexOfBar + 1)
         } catch (e: Exception) {
            ""
         }

         return SQSEnvelope(large, size, name, payload)
      }

      @JvmStatic
      fun main(args: Array<String>) {
         val envelopeSerialized = stringify(SQSEnvelope(false, 1000, "move.MyAction", "{}"))
         println(stringify(SQSEnvelope(false, 1000, "move.MyAction", "{}")))
         println(stringify(SQSEnvelope(true, 1000, "move.MyAction", "{}")))

         val envelope = parse(envelopeSerialized)

         println(envelope)
      }
   }
}
