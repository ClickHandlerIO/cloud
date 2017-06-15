package move.action

import com.google.common.base.Preconditions
import io.vertx.rxjava.core.Vertx
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

    /**
     * @param request
     * *
     * @return
     */
    open fun send(request: IN): Single<WorkerReceipt> {
        return send(request, 0)
    }

    /**
     * @param request
     * *
     * @param delaySeconds
     * *
     * @return
     */
    open fun send(request: IN, delaySeconds: Int): Single<WorkerReceipt> {
        Preconditions.checkNotNull(
                producer,
                "WorkerProducer is null. Ensure ActionManager has been started and all actions have been registered."
        )
        return producer!!.send(WorkerRequest()
                .actionProvider(this)
                .request(request)
                .delaySeconds(delaySeconds))
    }
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