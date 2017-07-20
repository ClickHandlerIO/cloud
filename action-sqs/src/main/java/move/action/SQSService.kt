package move.action

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.ExecutorFactory
import com.amazonaws.event.ProgressEvent
import com.amazonaws.event.ProgressEventType
import com.amazonaws.event.ProgressListener
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
import com.google.common.util.concurrent.AbstractIdleService
import com.netflix.hystrix.exception.HystrixTimeoutException
import io.vertx.rxjava.core.Vertx
import javaslang.control.Try
import move.common.Metrics
import move.common.UID
import move.common.WireFormat
import org.slf4j.LoggerFactory
import rx.Single
import java.io.File
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
                     val config: SQSConfig) : AbstractIdleService(), WorkerService {

   private val queueMap = HashMap<Class<*>, QueueContext>()

   private lateinit var realSQS: AmazonSQSAsync
   private lateinit var client: MoveAmazonSQSBufferedAsyncClient
   private lateinit var bufferExecutor: ExecutorService
   //   private lateinit var s3Client: AmazonS3Client
   private lateinit var s3TransferManager: TransferManager

   private val fileCache: Cache<String, File> = CacheBuilder
      .newBuilder()
      .maximumSize(50000)
      .expireAfterWrite(2, TimeUnit.HOURS)
      .removalListener(object : RemovalListener<String, File> {
         override fun onRemoval(notification: RemovalNotification<String, File>?) {
            if (notification?.wasEvicted() == true) {
               notification.value.delete()
            }
         }
      }).build()

   private val uploadCache: Cache<String, Upload> = CacheBuilder
      .newBuilder()
      .maximumSize(50000)
      .expireAfterWrite(2, TimeUnit.HOURS)
      .removalListener(object : RemovalListener<String, Upload> {
         override fun onRemoval(notification: RemovalNotification<String, Upload>?) {
            if (notification?.wasEvicted() == true) {
               notification.value.abort()
            }
         }
      }).build()

   private val downloadCache: Cache<String, Download> = CacheBuilder
      .newBuilder()
      .maximumSize(50000)
      .expireAfterWrite(2, TimeUnit.HOURS)
      .removalListener(object : RemovalListener<String, Download> {
         override fun onRemoval(notification: RemovalNotification<String, Download>?) {
            if (notification?.wasEvicted() == true) {
               notification.value.abort()
            }
         }
      }).build()

   private fun workerEnabled(provider: WorkerActionProvider<*, *>): Boolean {
      if (!config.worker) {
         return false
      }

      if (config.exclusions != null && !config.exclusions!!.isEmpty()) {
         return !config.exclusions!!.contains(provider.name) && !config.exclusions!!.contains(provider.queueName)
      }

      if (config.inclusions != null && !config.inclusions!!.isEmpty()) {
         return config.inclusions!!.contains(provider.name) || config.inclusions!!.contains(provider.queueName)
      }

      return true
   }

   private fun queueConfig(provider: WorkerActionProvider<*, *>): SQSQueueConfig {
      var queueConfig = queueConfig0(provider)

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

   private fun queueConfig0(provider: WorkerActionProvider<*, *>): SQSQueueConfig {
      if (config.queues == null || config.queues!!.isEmpty()) {
         return SQSQueueConfig()
      }

      return config.queues!!.stream()
         .filter {
            Strings.nullToEmpty(it.name).equals(provider.name, ignoreCase = true) ||
               Strings.nullToEmpty(it.name).equals(provider.queueName, ignoreCase = true)
         }
         .findFirst()
         .orElse(null)
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

      // Sanitize maxThreads property.
      config.maxThreads = if (config.maxThreads < MIN_THREADS) {
         LOG.warn("'maxThreads was set too low. Setting maxThreads to '" + MIN_THREADS + "'")
         MIN_THREADS
      } else if (config.maxThreads > MAX_THREADS) {
         LOG.warn("'maxThreads was set too high. Setting maxThreads to '" + MAX_THREADS + "'")
         MAX_THREADS
      } else
         config.maxThreads

      config.namespace = config.namespace?.trim() ?: ""

      // Create SQS QueueBuffer Executor.
      // Uses a capped cached thread strategy with the core size set to 0.
      // So it won't consume any threads if there isn't any activity.
      bufferExecutor = ThreadPoolExecutor(
         0,
         config.maxThreads,
         KEEP_ALIVE_SECONDS,
         TimeUnit.SECONDS,
         SynchronousQueue<Runnable>()
      )

      val builder = AmazonSQSAsyncClientBuilder
         .standard()
         .withExecutorFactory {
            bufferExecutor
         }
         .withRegion(config.region)

      config.awsAccessKey = config.awsAccessKey?.trim() ?: ""
      config.awsSecretKey = config.awsSecretKey?.trim() ?: ""

      config.s3AccessKey = config.s3AccessKey?.trim() ?: ""
      config.s3SecretKey = config.s3SecretKey?.trim() ?: ""

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
         .withMaxConnections(config.maxThreads * 2)
      )

      realSQS = builder.build()
      client = MoveAmazonSQSBufferedAsyncClient(realSQS)

      ActionManager.workerActionMap.forEach { key, provider ->
         val queueName = config.namespace + provider.queueName
         LOG.info("Calling getQueueUrl() for " + queueName)

         val result = realSQS.getQueueUrl(queueName)

         val queueUrl =
            if (result == null || result.queueUrl == null || result.queueUrl.isEmpty()) {
               val createQueueResult = realSQS.createQueue(queueName)
               createQueueResult.queueUrl
            } else {
               result.queueUrl
            }

         val queueConfig = queueConfig(provider)

         val bufferConfig = QueueBufferConfig()
            .withMaxBatchSize(queueConfig.maxBatchSize)
            .withMaxBatchOpenMs(queueConfig.maxBatchOpenMs.toLong())
            .withFlushOnShutdown(config.flushOnShutdown)

            // Turn off pre-fetching
            .withMaxInflightReceiveBatches(0)
            .withMaxDoneReceiveBatches(0)

         val isWorker = workerEnabled(provider)
         if (isWorker) {
            // Enable pre-fetching
            bufferConfig.withMaxInflightReceiveBatches(queueConfig.maxInflightReceiveBatches)
            bufferConfig.withMaxDoneReceiveBatches(queueConfig.maxDoneReceiveBatches)

            if (queueConfig.maxInflightReceiveBatches > 0 && queueConfig.maxDoneReceiveBatches > 0) {
               bufferConfig.withVisibilityTimeoutSeconds(
                  TimeUnit.MILLISECONDS.toSeconds((provider.getTimeoutMillis() * 2.1).toLong()).toInt()
               )
            } else {
               bufferConfig.withVisibilityTimeoutSeconds(
                  TimeUnit.MILLISECONDS.toSeconds((provider.getTimeoutMillis() + 2000)).toInt()
               )
            }
         }

         val buffer = client.putQBuffer(queueUrl, bufferConfig, bufferExecutor)

         val sender = Sender(
            provider,
            queueUrl,
            buffer
         )

         ActionManager.bindProducer(provider, sender)

         var receiver: Receiver? = null
         if (isWorker) {
            receiver = Receiver(
               vertx,
               provider,
               queueUrl,
               buffer,
               if (queueConfig.threads > 0)
                  queueConfig.threads
               else
                  0
            )
         }

         queueMap.put(provider.actionClass, QueueContext(sender, receiver, queueUrl, queueConfig))
      }

      // Startup all receivers.
      queueMap.values.forEach { queueContext ->
         if (queueContext.receiver != null) {
            try {
               queueContext.receiver.startAsync().awaitRunning()
            } catch (e: Throwable) {
               LOG.error("Failed to start Receiver for '" + queueContext.config.name + "'")
               throw RuntimeException(e)
            }

         }
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
   }

   data class Envelope(val l: Boolean, val s: Int? = null, val p: String)

   data class S3Pointer(val b: String, val k: String, val e: String)

   private inner class Sender(
      private val actionProvider: WorkerActionProvider<*, *>,
      private val queueUrl: String,
      private val buffer: QueueBuffer) : WorkerProducer {

      override fun send(request: WorkerRequest): Single<WorkerReceipt> {
         return Single.create<WorkerReceipt> { subscriber ->
            val payload = WireFormat.stringify(request.request)

            if (payload.length > MAX_PAYLOAD_SIZE || actionProvider.workerAction?.encrypted == true) {
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
               uploadCache.put(id, upload)
               val transfer = upload as AbstractTransfer
               transfer.addStateChangeListener { t, state ->
                  if (state == Transfer.TransferState.Completed) {
                     uploadCache.invalidate(id)
                     val uploadResult = upload.waitForUploadResult()

                     val sendRequest = SendMessageRequest(
                        queueUrl,
                        stringify(Envelope(
                           true,
                           payload.length,
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

                        if (request.groupId != null && !request.groupId.isEmpty()) {
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
                     uploadCache.invalidate(id)
                  }
               }
            } else {
               val sendRequest = SendMessageRequest(
                  queueUrl,
                  stringify(Envelope(false, payload.length, payload))
               )

               if (request.delaySeconds > 0) {
                  sendRequest.delaySeconds = request.delaySeconds
               }

               // Handle fifo
               if (actionProvider.isFifo) {
                  var groupId = actionProvider.name

                  if (request.groupId != null && !request.groupId.isEmpty()) {
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
   private inner class Receiver(val vertx: Vertx,
                                val actionProvider: WorkerActionProvider<Action<Any, Boolean>, Any>,
                                val queueUrl: String,
                                val sqsBuffer: QueueBuffer,
                                parallelism: Int) : AbstractIdleService() {
//        private var receiveThreads: Array<ReceiveThread?>? = null

      val registry = Metrics.registry()

      private val secondsSinceLastPollGauge: Gauge<Long> = try {
         registry.register<Gauge<Long>>(
                 actionProvider.name + "-SECONDS_SINCE_LAST_POLL",
                 Gauge<Long> { TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastPoll) }
         )
      } catch (e: Throwable) {
         registry.metrics[actionProvider.name + "-SECONDS_SINCE_LAST_POLL"] as Gauge<Long>
      }

      private val activeMessagesGauge: Gauge<Int> = try {
         registry.register<Gauge<Int>>(
                 actionProvider.name + "-ACTIVE_MESSAGES",
                 Gauge<Int> { activeMessages.get() }
         )
      } catch (e: Throwable) {
         registry.metrics[actionProvider.name + "-ACTIVE_MESSAGES"] as Gauge<Int>
      }
      private val receiveThreadsCounter: Counter = registry.counter(actionProvider.name + "-CONCURRENCY")
      private val jobsCounter: Counter = registry.counter(actionProvider.name + "-JOBS")
      private val timeoutsCounter: Counter = registry.counter(actionProvider.name + "-TIMEOUTS")
      private val completesCounter: Counter = registry.counter(actionProvider.name + "-COMPLETES")
      private val inCompletesCounter: Counter = registry.counter(actionProvider.name + "-IN_COMPLETES")
      private val exceptionsCounter: Counter = registry.counter(actionProvider.name + "-EXCEPTIONS")
      private val deletesCounter: Counter = registry.counter(actionProvider.name + "-DELETES")
      private val deleteFailuresCounter: Counter = registry.counter(actionProvider.name + "-DELETE_FAILURES")
      private val receiveLatencyCounter: Counter = registry.counter(actionProvider.name + "-RECEIVE_LATENCY")
      private val noMessagesCounter: Counter = registry.counter(actionProvider.name + "-NO_MESSAGES")
      private val downloadsCounter: Counter = registry.counter(actionProvider.name + "-DOWNLOADS")
      private val downloadsNanosCounter: Counter = registry.counter(actionProvider.name + "-DOWNLOAD_NANOS")
      private val downloadedBytesCounter: Counter = registry.counter(actionProvider.name + "-DOWNLOADED_BYTES")

      @Volatile private var lastPoll: Long = 0

      private val activeMessages = AtomicInteger(0)

      val concurrentRequests = if (parallelism < MIN_RECEIVE_THREADS) {
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
               LOG.error("Failed to delete message for receipt handle " + receiptHandle, exception)
               deleteFailuresCounter.inc()
            }

            override fun onSuccess(receiveRequest: DeleteMessageRequest, deleteMessageResult: DeleteMessageResult) {
               // Ignore.
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
         // Give it a 1 second poll
         request.waitTimeSeconds = 1

         if (request.maxNumberOfMessages <= 0)
            return

         lastPoll = Math.max(System.currentTimeMillis(), lastPoll)
         val start = System.currentTimeMillis()

         sqsBuffer.receiveMessage(request, object : AsyncHandler<ReceiveMessageRequest, ReceiveMessageResult> {
            override fun onError(exception: java.lang.Exception?) {
               receiveLatencyCounter.inc(System.currentTimeMillis() - start)
               LOG.error("ReceiveMessage for " + actionProvider.queueName + " threw an exception", exception)
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
                           val file = File.createTempFile("move_sqs_" + s3Pointer.k, "json")
                           fileCache.put(s3Pointer.k, file)

                           // Start S3 Download.
                           val download = s3TransferManager.download(s3Pointer.b, s3Pointer.k, file)
                           downloadCache.put(s3Pointer.k, download)

                           (download as AbstractTransfer).addStateChangeListener { transfer, state ->
                              if (state == Transfer.TransferState.Completed) {
                                 downloadCache.invalidate(s3Pointer.k)
                                 val length = file.length()
                                 downloadedBytesCounter.inc(length)
                                 downloadsNanosCounter.inc(System.nanoTime() - downloadStarted)

                                 if (length > config.maxPayloadSize) {
                                    if (file.delete()) {
                                       fileCache.invalidate(s3Pointer.k)
                                    }

                                    LOG.error("Payload of size '" + length + "' exceeded max allowable '" + config.maxPayloadSize + "'")
                                    return@addStateChangeListener
                                 }

                                 if (isRunning) {
                                    vertx.fileSystem().rxReadFile(file.absolutePath).subscribe(
                                       {
                                          val payload = it.toString()

                                          if (file.delete()) {
                                             fileCache.invalidate(s3Pointer.k)
                                          }

                                          vertx.runOnContext {
                                             call(message, payload)
                                          }
                                       },
                                       {
                                          LOG.error("Failed to read File '" + file.absoluteFile + "'", it)
                                          if (file.delete()) {
                                             fileCache.invalidate(s3Pointer.k)
                                          }
                                       }
                                    )
                                 }
                              } else if (state == Transfer.TransferState.Failed || state == Transfer.TransferState.Canceled) {
                                 downloadCache.invalidate(s3Pointer.k)
                              }
                           }
                        }.subscribe(
                           { f ->
                           },
                           {
                              LOG.error("Failed to Download", it)
                           }
                        )
                     }
                  } else {
                     call(message, envelope.p)
                  }
               }
            }
         })

         // Try to receive more.
         receiveMore()
      }

      fun call(message: Message, body: String) {
         if (!isRunning)
            return

         val request: Any = if (body.isEmpty())
            actionProvider.inProvider.get()
         else
            WireFormat.parse(
               actionProvider.inClass,
               body
            ) ?: actionProvider.inProvider.get()

         try {
            jobsCounter.inc()

            actionProvider.single(request).subscribe(
               {
                  activeMessages.decrementAndGet()

                  if (it != null && it.isSuccess) {
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
   private inner class QueueContext(val sender: Sender,
                                    val receiver: Receiver?,
                                    val queueUrl: String,
                                    val config: SQSQueueConfig)

   companion object {
      private val LOG = LoggerFactory.getLogger(SQSService::class.java)

      private val MIN_THREADS = 1
      private val MAX_THREADS = 1024
      private val KEEP_ALIVE_SECONDS = 60L
      private val MAX_PAYLOAD_SIZE = 255000
      private val MIN_RECEIVE_THREADS = 1
      private val MAX_RECEIVE_THREADS = 1024

      private val NULL_ENVELOPE = Envelope(false, 0, "")

      fun stringify(e: Envelope): String {
         val builder = StringBuilder()

         if (e.l)
            builder.append("1")
         else
            builder.append("0")

         return builder.append(e.s).append("|").append(e.p).toString()
      }

      fun parse(p: String): Envelope {
         if (p.isNullOrBlank()) {
            return NULL_ENVELOPE
         }

         val large = p[0] == '1'

         val indexOfBar = p.indexOf('|')
         if (indexOfBar < 2) {
            return NULL_ENVELOPE
         }

         val sizeSubstr = p.substring(1, indexOfBar)
         val size = try {
            Integer.parseInt(sizeSubstr)
         } catch (e: Exception) {
            0
         }
         val payload = if (indexOfBar + 1 == p.length) "" else try {
            p.substring(indexOfBar + 1)
         } catch (e: Exception) {
            ""
         }

         return Envelope(large, size, payload)
      }
   }
}
