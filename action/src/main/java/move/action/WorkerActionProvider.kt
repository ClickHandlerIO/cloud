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
            actionProvider: Provider<A>) : ActionProvider<A, IN, Boolean>(
   vertx, actionProvider
) {
   override val isWorker = true

   val annotation: Worker? = actionClass.getAnnotation(Worker::class.java)

   override val annotationTimeout: Int
      get() = annotation?.timeout ?: 0

   val name = if (annotation == null || annotation.queueName.isEmpty()) {
      actionClass.canonicalName
   } else {
      annotation.queueName
   }

   val isFifo = annotation?.fifo ?: false
   val queueName = name?.replace(".", "-")?.replace("action-worker", "") + if (isFifo) ".fifo" else ""

   val concurrency = annotation?.concurrency ?: 64

   internal var producer: WorkerProducer? = null

   /**
    *
    */
   fun blockingLocal(request: IN): Boolean {
      return create().rx(request).toBlocking().value()
   }

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
      val single = producer!!.send(WorkerRequest(
         actionProvider = this@WorkerActionProvider as WorkerActionProvider<Action<Any, Boolean>, Any>,
         delaySeconds = delaySeconds,
         request = request
      ))

      // Dispatch
      single.subscribe()

      return single
   }

   /**
    * @param request
    * *
    * @param groupId
    * *
    * @return
    */
   fun send(groupId: String, request: IN): Single<WorkerReceipt> {
      Preconditions.checkNotNull(
         producer,
         "WorkerProducer is null. Ensure ActionManager has been started and all actions have been registered."
      )
      return producer!!.send(WorkerRequest(
         actionProvider = this@WorkerActionProvider as WorkerActionProvider<Action<Any, Boolean>, Any>,
         groupId = groupId,
         request = request
      ))
   }

   /**
    * @param request
    * *
    * @param groupId
    * *
    * @return
    */
   fun send(request: IN, groupId: String): Single<WorkerReceipt> {
      Preconditions.checkNotNull(
         producer,
         "WorkerProducer is null. Ensure ActionManager has been started and all actions have been registered."
      )
      return producer!!.send(WorkerRequest(
         actionProvider = this@WorkerActionProvider as WorkerActionProvider<Action<Any, Boolean>, Any>,
         groupId = groupId,
         request = request
      ))
   }

   /**
    * @param request
    * *
    * @param delaySeconds
    * *
    * @return
    */
   fun send(groupId: String, delaySeconds: Int, request: IN): Single<WorkerReceipt> {
      Preconditions.checkNotNull(
         producer,
         "WorkerProducer is null. Ensure ActionManager has been started and all actions have been registered."
      )

      return producer!!.send(WorkerRequest(
         actionProvider = this@WorkerActionProvider as WorkerActionProvider<Action<Any, Boolean>, Any>,
         delaySeconds = delaySeconds,
         groupId = groupId,
         request = request
      ))
   }
}

/**
 *
 */
class WorkerReceipt @Inject constructor() {
   var mD5OfMessageBody: String? = null
   var mD5OfMessageAttributes: String? = null
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
   val isSuccess
      get() = !messageId.isNullOrBlank()
}
