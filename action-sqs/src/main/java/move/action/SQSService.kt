package move.action

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.http.AmazonHttpClient
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
import com.google.common.util.concurrent.AbstractExecutionThreadService
import com.google.common.util.concurrent.AbstractIdleService
import com.netflix.hystrix.exception.HystrixTimeoutException
import io.vertx.rxjava.core.Vertx
import javaslang.control.Try
import move.common.Metrics
import move.common.UID
import move.common.WireFormat
import org.slf4j.LoggerFactory
import rx.Single
import java.net.SocketException
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

    private var realSQS: AmazonSQSAsync? = null
    private var client: MoveAmazonSQSBufferedAsyncClient? = null
    private var bufferExecutor: ExecutorService? = null

    private fun workerEnabled(provider: WorkerActionProvider<*, *>): Boolean {
        if (!config.worker) {
            return false
        }

        if (config.exclusions != null && !config.exclusions.isEmpty()) {
            return !config.exclusions.contains(provider.name) && !config.exclusions.contains(provider.queueName)
        }

        if (config.inclusions != null && !config.inclusions.isEmpty()) {
            return config.inclusions.contains(provider.name) || config.inclusions.contains(provider.queueName)
        }

        return true
    }

    private fun queueConfig(provider: WorkerActionProvider<*, *>): SQSQueueConfig {
        var queueConfig = queueConfig0(provider)

        if (queueConfig == null) {
            queueConfig = SQSQueueConfig()
        }

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

    private fun queueConfig0(provider: WorkerActionProvider<*, *>): SQSQueueConfig? {
        if (config.queues == null || config.queues.isEmpty()) {
            return null
        }

        return config.queues.stream()
                .filter { Strings.nullToEmpty(it.name).equals(provider.name, ignoreCase = true) }
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

        // Set credentials if necessary.
        if (!config.awsAccessKey.isEmpty()) {
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

            val result = realSQS!!.getQueueUrl(queueName)

            val queueUrl =
                    if (result == null || result.queueUrl == null || result.queueUrl.isEmpty()) {
                        val createQueueResult = realSQS!!.createQueue(queueName)
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

            val buffer = client!!.putQBuffer(queueUrl, bufferConfig, bufferExecutor)

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

        client!!.shutdown()
    }

    private class Sender(
            private val actionProvider: WorkerActionProvider<*, *>,
            private val queueUrl: String,
            private val buffer: QueueBuffer) : WorkerProducer {

        override fun send(request: WorkerRequest): Single<WorkerReceipt> {
            return Single.create<WorkerReceipt> { subscriber ->
                val sendRequest = SendMessageRequest(
                        queueUrl,
                        WireFormat.stringify(request.request)
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

    /**
     * Processes Worker requests from a single AmazonSQS Queue.
     * Supports parallel receive and delete threads and utilizes SQS batching to increase message throughput.
     *
     *
     * Reliable
     */
    private class Receiver(val vertx: Vertx,
                           val actionProvider: WorkerActionProvider<Action<Any, Boolean>, Any>,
                           val queueUrl: String,
                           val sqsBuffer: QueueBuffer,
                           parallelism: Int) : AbstractIdleService() {
//        private var receiveThreads: Array<ReceiveThread?>? = null

        val registry = Metrics.registry()

        private val secondsSinceLastPollGauge: Gauge<Long> = registry.register<Gauge<Long>>(
                actionProvider.name + "-SECONDS_SINCE_LAST_POLL",
                Gauge<Long> { TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastPoll) }
        )
        private val activeMessagesGauge: Gauge<Int> = registry.register<Gauge<Int>>(
                actionProvider.name + "-ACTIVE_MESSAGES",
                Gauge<Int> { activeMessages.get() }
        )
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

        @Volatile private var lastPoll: Long = 0

        private val activeMessages = AtomicInteger(0)

        val activeList = CopyOnWriteArrayList<Single<Any>>()

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

//            var threads = actionProvider.maxConcurrentRequests
//
//            if (concurrentRequests > 0) {
//                threads = concurrentRequests
//            }
//
//            if (threads < MIN_RECEIVE_THREADS) {
//                threads = MIN_RECEIVE_THREADS
//            } else if (threads > MAX_RECEIVE_THREADS) {
//                threads = MAX_RECEIVE_THREADS
//            }

            // Start Receive threads.
//            receiveThreads = arrayOfNulls<ReceiveThread>(threads)
//            for (i in receiveThreads!!.indices) {
//                receiveThreads!![i] = ReceiveThread(i)
//                receiveThreads!![i]!!.startAsync().awaitRunning()
//            }
        }

        @Throws(Exception::class)
        override fun shutDown() {
            sqsBuffer.shutdown()

            // Stop receiving messages.
//            for (thread in receiveThreads!!) {
//                Try.run { thread!!.stopAsync().awaitTerminated(5, TimeUnit.SECONDS) }
//            }
//
//            // Clear threads.
//            receiveThreads = null
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
                        val body = Strings.nullToEmpty(message.body)
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

                                        if (it) {
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
                }
            })

            receiveMore()
        }

        private class InternalInterruptedException(cause: Throwable) : RuntimeException(cause)

        /**

         */
        private inner class ReceiveThread(private val number: Int) : AbstractExecutionThreadService() {
            private var thread: Thread? = null

            override fun serviceName(): String {
                return actionProvider.name + "-" + number
            }

            override fun triggerShutdown() {
                Try.run { thread!!.interrupt() }
            }

            @Throws(InterruptedException::class, SocketException::class)
            protected fun receiveMessage(request: ReceiveMessageRequest): ReceiveMessageResult {
                // Receive blocking.
                return sqsBuffer.receiveMessageSync(request)
            }

            @Throws(InterruptedException::class, SocketException::class)
            protected fun deleteMessage(receiptHandle: String) {
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

            @Throws(Exception::class)
            override fun run() {
                thread = Thread.currentThread()
                receiveThreadsCounter.inc()

                try {
                    while (isRunning) {
                        try {
                            var result: ReceiveMessageResult? = null
                            try {
                                lastPoll = Math.max(System.currentTimeMillis(), lastPoll)

                                val request = ReceiveMessageRequest()
                                        .withQueueUrl(queueUrl)
                                        //                                .withWaitTimeSeconds(20)
                                        //                                .withVisibilityTimeout((int) TimeUnit.MILLISECONDS.toSeconds(actionProvider.getTimeoutMillis() + 1000))
                                        .withMaxNumberOfMessages(1)

                                // Receive a batch of messages.
                                result = receiveMessage(request)
                            } catch (e: InterruptedException) {
                                LOG.warn("Interrupted.", e)
                                return
                            } catch (e: Exception) {
                                LOG.warn("SQS Consumer Exception", e)
                            }

                            if (result == null) {
                                continue
                            }

                            val messages = result.messages

                            // Were any messages received?
                            if (messages == null || messages.isEmpty()) {
                                continue
                            }

                            // Are we still running.
                            if (!isRunning) {
                                LOG.warn("Consumer not running when checked.")
                                return
                            }

                            for (message in messages) {
                                // Parse request.
                                val body = Strings.nullToEmpty(message.body)
                                val request: Any = if (body.isEmpty())
                                    actionProvider.inProvider.get()
                                else
                                    WireFormat.parse(
                                            actionProvider.inClass,
                                            body
                                    ) ?: actionProvider.inProvider.get()

                                try {
                                    jobsCounter.inc()

                                    val shouldDelete = actionProvider.executeBlocking(request)

                                    if (shouldDelete) {
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
                        } catch (e: Exception) {
                            if (e is InternalInterruptedException) {
                                LOG.info("Consumer Interrupted... Shutting down.")
                                return
                            }

                            // Ignore.
                            LOG.error("ReceiveThread.run() threw an exception", e)
                        }

                    }
                } finally {
                    receiveThreadsCounter.dec()
                }
            }
        }

        companion object {
            private val LOG = LoggerFactory.getLogger(Receiver::class.java)
            private val MIN_RECEIVE_THREADS = 1
            private val MAX_RECEIVE_THREADS = 1024
        }
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
    }
}
