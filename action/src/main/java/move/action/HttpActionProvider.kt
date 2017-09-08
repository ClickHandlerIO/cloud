package move.action

import io.vertx.ext.web.RoutingContext
import io.vertx.rxjava.core.Vertx
import kotlinx.coroutines.experimental.CoroutineStart
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider


open class HttpActionProvider<A : HttpAction>
@Inject constructor(vertx: Vertx, provider: Provider<A>)
   : ActionProvider<A, RoutingContext, Unit>(vertx, provider) {
   override val isHttp = true

   val annotation: Http? = actionClass.getAnnotation(Http::class.java)
   val visibility: ActionVisibility = annotation?.visibility ?: ActionVisibility.PUBLIC

   @Suppress("UNCHECKED_CAST")
   val self = this as HttpActionProvider<HttpAction>

   val annotationTimeout: Int
      get() = annotation?.timeout ?: 0

   private var executionTimeoutEnabled: Boolean = false
   internal var timeoutMillis: Int = annotationTimeout
   internal var timeoutMillisLong = timeoutMillis.toLong()

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


   fun createAsRoot(request: RoutingContext): A {
      return createAsRoot(request, timeoutMillisLong)
   }

   fun createAsRoot(request: RoutingContext, timeout: Long, unit: TimeUnit): A {
      return createAsRoot(request, unit.toMillis(timeout))
   }

   /**
    *
    */
   fun createAsRoot(request: RoutingContext, timeoutMillis: Long): A {
      // Create new Action instance.
      val action = actionProvider.get()

      // Get or create ActionContext.
      val eventLoop: MoveEventLoop = eventLoopGroup.next()
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

   fun create(request: RoutingContext): A {
      return createAsChild(request, timeoutMillisLong)
   }

   fun createAsChild(request: RoutingContext, timeout: Long, unit: TimeUnit): A {
      return createAsChild(request, unit.toMillis(timeout))
   }

   fun createAsChild(request: RoutingContext, timeoutMillis: Long): A {
      // Create new Action instance.
      val action = actionProvider.get()

      // Get or create ActionContext.
      val eventLoop: MoveEventLoop = eventLoopGroup.next()
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
}