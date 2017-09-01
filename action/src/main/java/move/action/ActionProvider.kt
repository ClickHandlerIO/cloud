package move.action

import io.vertx.core.impl.ActionEventLoopContext
import io.vertx.kotlin.circuitbreaker.CircuitBreakerOptions
import io.vertx.rxjava.core.Vertx
import rx.Single
import javax.inject.Provider

/**
 * Builds and invokes a single type of Action.

 * @author Clay Molocznik
 */
abstract class ActionProvider<A : Action<IN, OUT>, IN : Any, OUT : Any>
constructor(val vertx: Vertx, val actionProvider: Provider<A>) {
   val actionProviderClass = TypeResolver
      .resolveRawClass(
         ActionProvider::class.java,
         javaClass
      )

   @Suppress("UNCHECKED_CAST")
   val actionClass = TypeResolver
      .resolveRawArgument(
         actionProviderClass.typeParameters[0],
         javaClass
      ) as Class<A>

   @Suppress("UNCHECKED_CAST")
   val requestClass = TypeResolver
      .resolveRawArgument(
         actionProviderClass.typeParameters[1],
         javaClass
      ) as Class<IN>

   @Suppress("UNCHECKED_CAST")
   val replyClass = TypeResolver.resolveRawArgument(
      actionProviderClass.typeParameters[2],
      javaClass
   ) as Class<OUT>

   @Suppress("UNCHECKED_CAST")
   val self = this as ActionProvider<Action<IN, OUT>, IN, OUT>

   abstract val annotationTimeout: Int

   val vertxCore: io.vertx.core.Vertx = vertx.delegate

   val eventLoopGroup = ActionEventLoopGroup.get(vertx)

   private var executionTimeoutEnabled: Boolean = false
   private var timeoutMillis: Int = annotationTimeout
   var maxConcurrentRequests: Int = 0
      internal set

   var isExecutionTimeoutEnabled: Boolean
      get() = executionTimeoutEnabled
      set(enabled) {
      }

   var breaker: ActionCircuitBreaker = ActionCircuitBreaker(
      actionClass.name,
      vertx.delegate,
      CircuitBreakerOptions()
         .setMetricsRollingWindow(5000L)
         .setResetTimeout(2000L),
      eventLoopGroup
   )

   init {
      init()
   }

   fun getTimeoutMillis(): Long {
      return timeoutMillis.toLong()
   }

   fun timeoutMillis() = timeoutMillis

   open val isInternal = false
   open val isRemote = false
   open val isWorker = false
   open val isScheduled = false

   protected open fun init() {
      // Timeout milliseconds.
      var timeoutMillis = 0
      if (this.timeoutMillis > 0L) {
         timeoutMillis = this.timeoutMillis
      }

      this.timeoutMillis = timeoutMillis

      // Enable timeout?
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

   protected fun calcTimeout(action: A, context: ActionContext): Long {
      if (timeoutMillis < 1L) {
         return 0L
      }

      // Calculate max execution millis.
      val timesOutAt = System.currentTimeMillis() + timeoutMillis
      if (timesOutAt > context.currentTimeout) {
         return context.currentTimeout
      }

      return timesOutAt
   }

   fun create(): A {
      // Create new Action instance.
      val action = actionProvider.get()

      // Get or create ActionContext.
      var context: ActionContext? = Action.contextLocal.get()

      val timesOutAt: Long
      if (context == null) {
         val eventLoop: ActionEventLoopContext = eventLoopGroup.next()

         context = ActionContext(
            System.currentTimeMillis(),
            timeoutMillis.toLong(),
            this,
            eventLoop
         )
         timesOutAt = context.timesOutAt
      } else {
         timesOutAt = calcTimeout(action, context)
      }

      action.init(
         self,
         context,
         timesOutAt
      )

      // Return action instance.
      return action
   }

   internal fun create(request: IN): A {
      // Create new Action instance.
      val action = create()
      action._request = request

      // Return action instance.
      return action
   }

   internal fun create(data: Any?, request: IN): A {
      // Create new Action instance.
      val action = create()
      action._request = request
      action.context.data = data

      // Return action instance.
      return action
   }

   open fun rx(request: IN): Single<OUT> {
      return create().rx(request)
   }

   companion object {
      internal val MIN_TIMEOUT_MILLIS = 200
      internal val MILLIS_TO_SECONDS_THRESHOLD = 500
      internal val DEFAULT_CONCURRENCY_INTERNAL = 10000
      internal val DEFAULT_CONCURRENCY_REMOTE = 10000
      internal val DEFAULT_CONCURRENCY_WORKER = 10000
      internal val DEFAULT_CONCURRENCY_SCHEDULED = 1

      fun current(): ActionContext? {
         return Action.currentContext()
      }
   }
}
