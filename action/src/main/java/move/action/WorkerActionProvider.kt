package move.action

import com.google.common.base.Preconditions
import io.vertx.rxjava.core.Vertx
import kotlinx.coroutines.experimental.CoroutineStart
import rx.Single
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider

/**

 */
open class WorkerActionProvider<A : WorkerAction<IN, OUT>, IN : Any, OUT : Any> @Inject
constructor(vertx: Vertx,
            actionProvider: Provider<A>) : ActionProvider<A, IN, OUT>(vertx, actionProvider) {

   override val isWorker = true

   val annotation: Worker? = actionClass.getAnnotation(Worker::class.java)
   val annotationTimeout = annotation?.timeout ?: 0

   @Suppress("UNCHECKED_CAST")
   val self = this as WorkerActionProvider<WorkerAction<IN, OUT>, IN, OUT>

   val visibility: ActionVisibility
      get() = annotation?.visibility ?: ActionVisibility.PRIVATE

   private var executionTimeoutEnabled: Boolean = false
   internal var timeoutMillis: Int = annotationTimeout
   internal var timeoutMillisLong = timeoutMillis.toLong()

   var maxConcurrentRequests: Int = 0
      internal set

   var isExecutionTimeoutEnabled: Boolean
      get() = executionTimeoutEnabled
      set(enabled) {
      }

   private var queueGroup: String = ""

   val isFifo = annotation?.fifo ?: false
   val queueName = name?.replace(".", "-")?.replace("action-worker", "") + if (isFifo) ".fifo" else ""

   internal var producer: WorkerProducer? = null

   override fun init() {
      name = findName(annotation?.value)
      queueGroup = name

      // Timeout milliseconds.
      var timeoutMillis = 0
      if (this.timeoutMillis > 0L) {
         timeoutMillis = this.timeoutMillis
      }

      this.timeoutMillis = timeoutMillis

      // Enable deadline?
      if (timeoutMillis > 0) {
         if (timeoutMillis < MILLIS_TO_SECONDS_THRESHOLD) {
            // Looks like somebody decided to put seconds instead of milliseconds.
            timeoutMillis = timeoutMillis * 1000
         } else if (timeoutMillis < 1000) {
            timeoutMillis = 1000
         }

         this.timeoutMillis = timeoutMillis
         executionTimeoutEnabled = true
      }
   }

   protected open fun calcTimeout(timeoutMillis: Long, context: IActionContext): Long {
      if (timeoutMillis < 1) {
         return context.deadline
      }

      // Calculate max execution millis.
      val deadline = System.currentTimeMillis() + timeoutMillis
      if (deadline > context.deadline) {
         return context.deadline
      }

      return deadline
   }


   fun createAsRoot(request: IN): A {
      return createAsRoot(request, timeoutMillisLong)
   }

   fun createAsRoot(request: IN, timeout: Long, unit: TimeUnit): A {
      return createAsRoot(request, unit.toMillis(timeout))
   }

   /**
    *
    */
   fun createAsRoot(request: IN, timeoutMillis: Long): A {
      // Create new Action instance.
      val action = actionProvider.get()

      // Get or create ActionContext.
      val eventLoop: ActionEventLoopContext = eventLoopGroup.next()
      action.init(
         eventLoop.dispatcher,
         self,
         request,
         timeoutMillis,
         CoroutineStart.DEFAULT,
         false
      )

      // Return action instance.
      return action
   }

   fun create(request: IN): A {
      return createAsChild(request, timeoutMillisLong)
   }

   fun createAsChild(request: IN, timeout: Long, unit: TimeUnit): A {
      return createAsChild(request, unit.toMillis(timeout))
   }

   fun createAsChild(request: IN, timeoutMillis: Long): A {
      // Create new Action instance.
      val action = actionProvider.get()

      // Get or create ActionContext.
      val eventLoop: ActionEventLoopContext = eventLoopGroup.next()
      action.init(
         eventLoop.dispatcher,
         self,
         request,
         timeoutMillis,
         CoroutineStart.DEFAULT,
         true
      )

      // Return action instance.
      return action
   }


//   /**
//    * @param request
//    * *
//    * @return
//    */
//   fun send(request: IN): Single<WorkerReceipt> = send(0, request)
//
//   /**
//    * @param request
//    * *
//    * @param delaySeconds
//    * *
//    * @return
//    */
//   fun send(delaySeconds: Int, request: IN): Single<WorkerReceipt> {
//      Preconditions.checkNotNull(
//         producer,
//         "WorkerProducer is null. Ensure ActionManager has been started and all actions have been registered."
//      )
//      val single = producer!!.send(WorkerRequest(
//         actionProvider = self,
//         delaySeconds = delaySeconds,
//         request = request
//      ))
//
//      // Dispatch
//      single.subscribe()
//
//      return single
//   }
//
//   /**
//    * @param request
//    * *
//    * @param groupId
//    * *
//    * @return
//    */
//   fun send(groupId: String, request: IN): Single<WorkerReceipt> {
//      Preconditions.checkNotNull(
//         producer,
//         "WorkerProducer is null. Ensure ActionManager has been started and all actions have been registered."
//      )
//      return producer!!.send(WorkerRequest(
//         actionProvider = this@WorkerActionProvider as WorkerActionProvider<WorkerAction<*, *>, *, *>,
//         groupId = groupId,
//         request = request
//      ))
//   }
//
//   /**
//    * @param request
//    * *
//    * @param groupId
//    * *
//    * @return
//    */
//   fun send(request: IN, groupId: String): Single<WorkerReceipt> {
//      Preconditions.checkNotNull(
//         producer,
//         "WorkerProducer is null. Ensure ActionManager has been started and all actions have been registered."
//      )
//      return producer!!.send(WorkerRequest(
//         actionProvider = this@WorkerActionProvider as WorkerActionProvider<WorkerAction<*, *>, *, *>,
//         groupId = groupId,
//         request = request
//      ))
//   }
//
//   /**
//    * @param request
//    * *
//    * @param delaySeconds
//    * *
//    * @return
//    */
//   fun send(groupId: String, delaySeconds: Int, request: IN): Single<WorkerReceipt> {
//      Preconditions.checkNotNull(
//         producer,
//         "WorkerProducer is null. Ensure ActionManager has been started and all actions have been registered."
//      )
//
//      return producer!!.send(WorkerRequest(
//         actionProvider = this@WorkerActionProvider as WorkerActionProvider<WorkerAction<*, *>, *, *>,
//         delaySeconds = delaySeconds,
//         groupId = groupId,
//         request = request
//      ))
//   }
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
