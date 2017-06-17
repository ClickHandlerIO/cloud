package move.action

import com.google.common.base.Preconditions
import io.vertx.rxjava.core.Vertx
import kotlinx.coroutines.experimental.rx1.await
import rx.Single
import javax.inject.Inject
import javax.inject.Provider

/**

 */
open class WorkerActionProvider<A : Action<IN, Boolean>, IN : Any> @Inject
constructor(vertx: Vertx,
            actionProvider: Provider<A>,
            inProvider: Provider<IN>) : ActionProvider<A, IN, Boolean>(
        vertx, actionProvider, inProvider, Provider<Boolean> { false }
) {
    override val isWorker = true

    val workerAction: WorkerAction? = actionClass.getAnnotation(WorkerAction::class.java)
    val name = actionClass.canonicalName
    val isFifo = workerAction?.fifo ?: false
    val queueName = name.replace(".", "-")

    internal var producer: WorkerProducer? = null

    fun blockingLocal(request: IN): Boolean {
        return super.blockingBuilder(request)
    }

    /**
     *
     */
    fun single(request: IN): Single<WorkerReceipt> = single(0, request)

    /**
     *
     */
    fun single(delaySeconds: Int, request: IN): Single<WorkerReceipt> =
            producer!!.send(WorkerRequest()
                    .actionProvider(this)
                    .request(request)
                    .delaySeconds(delaySeconds))

    /**
     *
     */
    fun single(block: IN.() -> Unit): Single<WorkerReceipt> = single(0, block)

    /**
     *
     */
    fun single(delaySeconds: Int, block: IN.() -> Unit): Single<WorkerReceipt> =
            single(delaySeconds, inProvider.get().apply(block))

    /**
     * @param request
     * *
     * @return
     */
    fun send(request: IN): Single<WorkerReceipt> = send(0, request)

    /**
     * @param request
     * *
     * @param delaySeconds
     * *
     * @return
     */
    fun send(delaySeconds: Int, request: IN): Single<WorkerReceipt> {
        Preconditions.checkNotNull(
                producer,
                "WorkerProducer is null. Ensure ActionManager has been started and all actions have been registered."
        )
        val single = producer!!.send(WorkerRequest()
                .actionProvider(this)
                .request(request)
                .delaySeconds(delaySeconds))

        // Dispatch
        single.subscribe()

        return single
    }

    /**
     * @param request
     * *
     * @return
     */
    fun send(block: IN.() -> Unit): Single<WorkerReceipt> = send(0, inProvider.get().apply(block))

    /**
     * @param request
     * *
     * @return
     */
    fun send(delaySeconds: Int, block: IN.() -> Unit): Single<WorkerReceipt> = send(delaySeconds, inProvider.get().apply(block))

    suspend operator fun invoke(request: IN): Single<WorkerReceipt> = send(request)

    suspend operator fun invoke(delaySeconds: Int, request: IN): Single<WorkerReceipt> = send(delaySeconds, request)

    suspend operator fun invoke(block: IN.() -> Unit): Single<WorkerReceipt> = send(0, block)

    suspend operator fun invoke(delaySeconds: Int, block: IN.() -> Unit): Single<WorkerReceipt> =
            send(delaySeconds, inProvider.get().apply(block))

    suspend fun await(request: IN): WorkerReceipt = single(request).await()

    suspend fun await(delaySeconds: Int, request: IN): WorkerReceipt = single(delaySeconds, request).await()

    suspend fun await(block: IN.() -> Unit): WorkerReceipt =
            single(inProvider.get().apply(block)).await()

    suspend fun await(delaySeconds: Int, block: IN.() -> Unit): WorkerReceipt =
            single(delaySeconds, inProvider.get().apply(block)).await()
}

class WorkerReceipt @Inject constructor() {
    /**
     * <p>
     * An MD5 digest of the non-URL-encoded message attribute string. You can use this attribute to verify that Amazon
     * SQS received the message correctly. Amazon SQS URL-decodes the message before creating the MD5 digest. For
     * information on MD5, see <a href="https://www.ietf.org/rfc/rfc1321.txt">RFC1321</a>.
     * </p>
     */
    var mD5OfMessageBody: String? = null
    /**
     * <p>
     * An MD5 digest of the non-URL-encoded message attribute string. You can use this attribute to verify that Amazon
     * SQS received the message correctly. Amazon SQS URL-decodes the message before creating the MD5 digest. For
     * information on MD5, see <a href="https://www.ietf.org/rfc/rfc1321.txt">RFC1321</a>.
     * </p>
     */
    var mD5OfMessageAttributes: String? = null
    /**
     * <p>
     * An attribute containing the <code>MessageId</code> of the message sent to the queue. For more information, see <a
     * href
     * ="http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-queue-message-identifiers.html"
     * >Queue and Message Identifiers</a> in the <i>Amazon SQS Developer Guide</i>.
     * </p>
     */
    var messageId: String? = null
    /**
     * <p>
     * This parameter applies only to FIFO (first-in-first-out) queues.
     * </p>
     * <p>
     * A large, non-consecutive number that Amazon SQS assigns to each message.
     * </p>
     * <p>
     * The length of <code>SequenceNumber</code> is 128 bits. <code>SequenceNumber</code> continues to increase for a
     * particular <code>MessageGroupId</code>.
     * </p>
     */
    var sequenceNumber: String? = null

    /**
     *
     */
    val isSuccess = messageId != null && messageId!!.isNotEmpty()
}