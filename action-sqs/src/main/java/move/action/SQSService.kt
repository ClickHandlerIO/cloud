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
import com.google.common.util.concurrent.AbstractIdleService
import com.google.common.util.concurrent.Service
import com.netflix.hystrix.exception.HystrixTimeoutException
import io.vertx.rxjava.core.Vertx
import javaslang.control.Try
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
 * Amazon SQS ActionBroker.
 *
 * Pulls ActionJob messages from SQS and dispatches onto ActionBus
 * using the "ask reliable" pattern with the action's subject.
 *
 * Visibility is adjusted if needed.
 *
 * @author Clay Molocznik
 */
@Singleton
class SQSService @Inject
internal constructor(val vertx: Vertx,
                     val config: SQSConfig) : AbstractIdleService() {

   private val queueMap = HashMap<String, QueueContext>()

   private lateinit var realSQS: AmazonSQSAsync
   private lateinit var client: MoveAmazonSQSBufferedAsyncClient
   private lateinit var bufferExecutor: ExecutorService
   //   private lateinit var s3Client: AmazonS3Client
   private lateinit var s3TransferManager: TransferManager
   private var receiveTimerID: Long = 0L
   private var fileTimerID: Long = 0L

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

   private fun workerEnabled(provider: WorkerActionProvider<*, *, *>): Boolean {
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

      // Set a good limit to max HTTP connections in SQS client.
//      val maxConnections = ActionManager.workerActionQueueGroupMap.map { it.value.size }.sum() * 2

      builder.withClientConfiguration(ClientConfiguration()
         // WorkerPacker takes care of GZIP compression already.
         .withGzip(false)
         .withThrottledRetries(true)
//         .withMaxConnections(Math.max(MIN_MAX_CONNECTIONS, maxConnections))
      )

      // Build SQS client and the Buffered client based on the "Real" SQS client.
      realSQS = builder.build()
      client = MoveAmazonSQSBufferedAsyncClient(realSQS)

//      Actions.workers.forEach { entry ->
//         val queueName = config.namespace + entry.key
//         val queueMetricName = "action-worker-queue" + entry.key
//         LOG.info("Calling getQueueUrl() for " + queueName)
//
//         val result = realSQS.getQueueUrl(queueName)
//
//         val queueUrl =
//            if (result == null || result.queueUrl == null || result.queueUrl.isEmpty()) {
//               val createQueueResult = realSQS.createQueue(queueName)
//               createQueueResult.queueUrl
//            } else {
//               result.queueUrl
//            }
//
//         // If there are more than 1 action mapped to this queue then find the max "concurrency"
//         val maxConcurrency = entry.value.map {
//            it.concurrency
//         }.max()?.toInt() ?: DEFAULT_CONCURRENCY
//         // If there are more than 1 action mapped to this queue then find largest "deadline"
//         val maxExecutionMillis = entry.value.map {
//            it.timeoutMillis()
//         }.max()?.toInt() ?: DEFAULT_WORKER_TIMEOUT_MILLIS
//         // Determine if any action in the group is enabled as a worker.
//         val workerEnabled = isWorkerEnabled && entry.value.find { workerEnabled(it) } != null
//
//         val queueConfig = queueConfig(queueName)
//
//         val bufferConfig = QueueBufferConfig()
//            .withMaxBatchSize(queueConfig.maxBatchSize)
//            .withMaxBatchOpenMs(queueConfig.maxBatchOpenMs.toLong())
//            .withFlushOnShutdown(config.flushOnShutdown)
//
//            // Turn off pre-fetching
//            .withMaxInflightReceiveBatches(0)
//            .withMaxDoneReceiveBatches(0)
//
//         if (workerEnabled) {
//            // Enable pre-fetching
//            bufferConfig.withLongPoll(true)
//            bufferConfig.withLongPollWaitTimeoutSeconds(SQS_POLL_WAIT_SECONDS)
//            bufferConfig.withMaxInflightReceiveBatches(queueConfig.maxInflightReceiveBatches)
//            bufferConfig.withMaxDoneReceiveBatches(queueConfig.maxDoneReceiveBatches)
//
//            if (queueConfig.maxInflightReceiveBatches > 0 && queueConfig.maxDoneReceiveBatches > 0) {
//               bufferConfig.withVisibilityTimeoutSeconds(
//                  TimeUnit.MILLISECONDS.toSeconds((maxExecutionMillis * VISIBILITY_MULTIPLIER).toLong()).toInt()
//               )
//            } else {
//               bufferConfig.withVisibilityTimeoutSeconds(
//                  TimeUnit.MILLISECONDS.toSeconds((maxExecutionMillis + VISIBILITY_PADDING_MILLIS)).toInt()
//               )
//            }
//         }
//
//         val buffer = client.putQBuffer(queueUrl, bufferConfig, bufferExecutor)
//
//         val sender = SQSQueueSender(
//            queueUrl,
//            buffer
//         )
//
//         entry.value.forEach { ActionManager.bindProducer(it, sender) }
//
//         val receiver: SQSQueueReceiver? = if (workerEnabled) {
//            SQSQueueReceiver(
//               vertx,
//               queueName,
//               queueMetricName,
//               queueUrl,
//               buffer,
//               if (queueConfig.parallelism > 0)
//                  queueConfig.parallelism
//               else
//                  0
//            )
//         } else {
//            null
//         }
//
//         queueMap.put(
//            queueName,
//            QueueContext(sender, receiver, queueUrl, queueConfig, ImmutableSet.of(entry.value))
//         )
//      }

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

      val hasReceivers = queueMap.filter { it.value.receiver != null }.count()

      // Set a periodic timer if there are receivers to ensure
      receiveTimerID =
         if (hasReceivers < 1)
            0L
         else
            vertx.setPeriodic(ENSURE_RECEIVERS_POLL_MILLIS) {
               queueMap.values.forEach {
                  it.receiver?.receiveMore()
               }
            }

      fileTimerID = vertx.setPeriodic(FILE_RETRY_AGE_SECONDS * 1000L) {
         cleanUpCaches()
      }
   }

   @Throws(Exception::class)
   override fun shutDown() {
      if (receiveTimerID != 0L) {
         vertx.cancelTimer(receiveTimerID)
      }

      queueMap.values.forEach { queueContext ->
         if (queueContext.receiver != null) {
            queueContext.receiver.stopAsync().awaitTerminated()
         }
      }

      client.shutdown()
      s3TransferManager.shutdownNow(true)

      if (fileTimerID != 0L) {
         vertx.cancelTimer(fileTimerID)
         cleanUpCaches()
      }
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
            val worker: Worker = if (actionProvider?.annotation != null) {
               actionProvider.annotation!!
            } else {
               return@create
            }

            val payload = WireFormat.byteify(request.request)

            if (payload.size > MAX_PAYLOAD_SIZE) {
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
                           System.currentTimeMillis(),
                           request.delaySeconds,
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
                  WorkerPacker.stringify(WorkerEnvelope(
                     0,
                     System.currentTimeMillis(),
                     request.delaySeconds,
                     actionProvider.name,
                     payload.size,
                     payload
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
                                        val queueMetricName: String,
                                        val queueUrl: String,
                                        val sqsBuffer: QueueBuffer,
                                        concurrency: Int) : AbstractIdleService() {
      val registry = Metrics.registry()

      private val secondsSinceLastPollGauge: Gauge<Long> = try {
         registry.register<Gauge<Long>>(
            queueMetricName + "-SECONDS_SINCE_LAST_POLL",
            Gauge<Long> { TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastPoll) }
         )
      } catch (e: Throwable) {
         registry.metrics[queueMetricName + "-SECONDS_SINCE_LAST_POLL"] as Gauge<Long>
      }

      private val activeMessagesGauge: Gauge<Int> = try {
         registry.register<Gauge<Int>>(
            queueMetricName + "-ACTIVE_MESSAGES",
            Gauge<Int> { activeMessages.get() }
         )
      } catch (e: Throwable) {
         registry.metrics[queueMetricName + "-ACTIVE_MESSAGES"] as Gauge<Int>
      }
      private val concurrencyGauge: Gauge<Int> = try {
         registry.register<Gauge<Int>>(
            queueMetricName + "-CONCURRENCY",
            Gauge<Int> { concurrentRequests }
         )
      } catch (e: Throwable) {
         registry.metrics[queueMetricName + "-PARALLELISM"] as Gauge<Int>
      }
      private val jobsCounter: Counter = registry.counter(queueMetricName + "-JOBS")
      private val timeoutsCounter: Counter = registry.counter(queueMetricName + "-TIMEOUTS")
      private val completesCounter: Counter = registry.counter(queueMetricName + "-COMPLETES")
      private val inCompletesCounter: Counter = registry.counter(queueMetricName + "-INCOMPLETES")
      private val exceptionsCounter: Counter = registry.counter(queueMetricName + "-EXCEPTIONS")
      private val deletesCounter: Counter = registry.counter(queueMetricName + "-DELETES")
      private val deleteFailuresCounter: Counter = registry.counter(queueMetricName + "-DELETE_FAILURES")
      private val receiveLatencyCounter: Counter = registry.counter(queueMetricName + "-RECEIVE_LATENCY")
      private val noMessagesCounter: Counter = registry.counter(queueMetricName + "-NO_MESSAGES")
      private val downloadsCounter: Counter = registry.counter(queueMetricName + "-DOWNLOADS")
      private val downloadsNanosCounter: Counter = registry.counter(queueMetricName + "-DOWNLOAD_NANOS")
      private val downloadedBytesCounter: Counter = registry.counter(queueMetricName + "-DOWNLOADED_BYTES")

      @Volatile private var lastPoll: Long = 0

      private val activeMessages = AtomicInteger(0)

      var concurrentRequests = if (concurrency < MIN_CONCURRENCY) {
         MIN_CONCURRENCY
      } else if (concurrency > MAX_CONCURRENCY) {
         MAX_CONCURRENCY
      } else {
         concurrency
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
         // The visibility deadline buffer provides a good safety net.
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
                     // Parse _request.
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

         val actionProvider = Actions[envelope.name]
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
               WireFormat.parse(actionProvider.requestClass, "{}")
            else
               WireFormat.parse(
                  actionProvider.requestClass,
                  body
               ) ?: WireFormat.parse(actionProvider.requestClass, "{}")

            jobsCounter.inc()

//            actionProvider.rx(request).subscribe(
//               {
//                  activeMessages.decrementAndGet()
//                  Try.run { receiveMore() }
//
//                  if (it != null && it) {
//                     completesCounter.inc()
//                     deletesCounter.inc()
//
//                     try {
//                        deleteMessage(message.receiptHandle)
//                     } catch (e: Throwable) {
//                        LOG.error("Delete message from queue '" + queueUrl + "' with receipt handle '" + message.receiptHandle + "' failed", e)
//                        deleteFailuresCounter.inc()
//                     }
//                  } else {
//                     inCompletesCounter.inc()
//                  }
//               },
//               {
//                  activeMessages.decrementAndGet()
//                  Try.run { receiveMore() }
//                  LOG.error("Action " + actionProvider.actionClass.canonicalName + " threw an exception", it)
//
//                  val rootCause = Throwables.getRootCause(it)
//                  if (rootCause is HystrixTimeoutException || rootCause is TimeoutException) {
//                     timeoutsCounter.inc()
//                  } else {
//                     exceptionsCounter.inc()
//                  }
//               }
//            )
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
                                    val config: SQSQueueConfig)

   companion object {
      private val LOG = LoggerFactory.getLogger(SQSService::class.java)

      private val MIN_MAX_CONNECTIONS = 100
      private val MAX_NUMBER_OF_MESSAGES = 10
      private val ENSURE_RECEIVERS_POLL_MILLIS = 2500L
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
      private val MIN_CONCURRENCY = 1
      private val MAX_CONCURRENCY = 1024 * 10
      private val DEFAULT_CONCURRENCY = Runtime.getRuntime().availableProcessors() * 2
      private val DEFAULT_WORKER_TIMEOUT_MILLIS = 30000
   }
}
