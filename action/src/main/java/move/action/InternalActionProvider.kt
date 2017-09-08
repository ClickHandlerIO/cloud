package move.action

import io.vertx.rxjava.core.Vertx
import kotlinx.coroutines.experimental.CoroutineStart
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider

/**

 */
open class InternalActionProvider<A : InternalAction<IN, OUT>, IN : Any, OUT : Any> @Inject
constructor(vertx: Vertx,
            actionProvider: Provider<A>) : ActionProvider<A, IN, OUT>(
   vertx, actionProvider
) {
   override val isInternal = true

   val annotation: Internal? = actionClass.getAnnotation(Internal::class.java)

   @Suppress("UNCHECKED_CAST")
   val self = this as InternalActionProvider<InternalAction<IN, OUT>, IN, OUT>

   val annotationTimeout: Int
      get() = annotation?.timeout ?: 0

   private var executionTimeoutEnabled: Boolean = false
   internal var timeoutMillis: Int = annotationTimeout
   internal var timeoutMillisLong = timeoutMillis.toLong()

   var maxConcurrentRequests: Int = 0
      internal set

   var isExecutionTimeoutEnabled: Boolean
      get() = executionTimeoutEnabled
      set(enabled) {
      }


   override fun init() {
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

   protected open fun deadline(timeoutMillis: Long): Long {
      // Force No-Timeout.
      if (timeoutMillis < 0)
         return 0L

      // Use default timeout.
      if (timeoutMillis == 0L) {
         return if (timeoutMillisLong > 0L)
            System.currentTimeMillis() + timeoutMillisLong
         else
            0
      }

      // Calculate deadline.
      return System.currentTimeMillis() + timeoutMillis
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
      val eventLoop: MoveEventLoop = eventLoopGroup.next()
      action.init(
         eventLoop.dispatcher,
         self,
         request,
         deadline(timeoutMillis),
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
      val eventLoop: MoveEventLoop = eventLoopGroup.next()
      action.init(
         eventLoop.dispatcher,
         self,
         request,
         deadline(timeoutMillis),
         CoroutineStart.DEFAULT,
         true
      )

      // Return action instance.
      return action
   }
}
