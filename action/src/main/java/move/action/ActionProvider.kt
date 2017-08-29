package move.action

import com.google.common.base.Throwables
import io.vertx.core.impl.ActionEventLoopContext
import io.vertx.kotlin.circuitbreaker.CircuitBreakerOptions
import io.vertx.rxjava.core.Vertx
import javaslang.control.Try
import rx.Observable
import rx.Single
import java.util.function.Consumer
import javax.inject.Inject
import javax.inject.Provider

/**
 * Builds and invokes a single type of Action.

 * @author Clay Molocznik
 */
open class ActionProvider<A : Action<IN, OUT>, IN : Any, OUT : Any> @Inject
constructor(
   val vertx: Vertx,
   val actionProvider: Provider<A>,
   val inProvider: Provider<IN>,
   val outProvider: Provider<OUT>) {
   val vertxCore: io.vertx.core.Vertx = vertx.delegate
   val inClass = inProvider.get().javaClass
   val outClass = outProvider.get().javaClass
   val actionClass = actionProvider.get().javaClass
   val actionConfig: ActionConfig? = actionClass.getAnnotation(ActionConfig::class.java)

   val eventLoopGroup = ActionEventLoopGroup.get(vertx)

   private var executionTimeoutEnabled: Boolean = false
   private var timeoutMillis: Int = 0
   var maxConcurrentRequests: Int = 0
      internal set

   var isExecutionTimeoutEnabled: Boolean
      get() = executionTimeoutEnabled
      set(enabled) {
      }

   var breaker: ActionCircuitBreaker = ActionCircuitBreaker(
      actionClass.canonicalName,
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

   open fun concurrency(): Int {
      if (isScheduled) {
         return DEFAULT_CONCURRENCY_SCHEDULED
      }

      return if (actionConfig == null) {
         if (isInternal) {
            return DEFAULT_CONCURRENCY_INTERNAL
         }
         if (isRemote) {
            return DEFAULT_CONCURRENCY_REMOTE
         }
         if (isWorker) {
            return DEFAULT_CONCURRENCY_WORKER
         }
         if (isScheduled) {
            return DEFAULT_CONCURRENCY_SCHEDULED
         }

         return DEFAULT_CONCURRENCY_INTERNAL
      } else {
         val p = actionConfig.concurrency
         if (p == ActionConfig.DEFAULT_CONCURRENCY) {
            if (isInternal) {
               return DEFAULT_CONCURRENCY_INTERNAL
            }
            if (isRemote) {
               return DEFAULT_CONCURRENCY_REMOTE
            }
            if (isWorker) {
               return DEFAULT_CONCURRENCY_WORKER
            }

            return DEFAULT_CONCURRENCY_INTERNAL
         } else {
            return p
         }
      }
   }

   protected open fun init() {
      // Timeout milliseconds.
      var timeoutMillis = ActionConfig.DEFAULT_TIMEOUT_MILLIS
      if (actionConfig != null) {
         timeoutMillis = actionConfig.timeoutMillis
      }

      this.timeoutMillis = timeoutMillis

      // Enable timeout?
      if (timeoutMillis > 0) {
         if (timeoutMillis < MILLIS_TO_SECONDS_THRESHOLD) {
            // Looks like somebody decided to put seconds instead of milliseconds.
            timeoutMillis = timeoutMillis * 1000
         } else if (timeoutMillis < 1000) {
            timeoutMillis = ActionConfig.DEFAULT_TIMEOUT_MILLIS
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
         this as ActionProvider<Action<IN, OUT>, IN, OUT>,
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

   /**
    * @return
    */
   open internal fun execute0(callable: Try.CheckedConsumer<IN>): OUT {
      val request = inProvider.get()
      Try.run { callable.accept(request) }
      return single0(request).toBlocking().value()
   }

   /**
    * @param request
    * *
    * @return
    */
   @Deprecated("")
   open internal fun execute0(request: IN): OUT {
      try {
         return single0(request).toBlocking().value()
      } catch (e: Throwable) {
         Throwables.throwIfUnchecked(e)
         throw RuntimeException(e)
      }
   }

   open internal fun blockingBuilder(request: IN): OUT = single0(request).toBlocking().value()

   open internal fun blockingBuilder(data: Any?, request: IN): OUT = single0(data, request).toBlocking().value()

   /**
    * @return
    */
   open internal fun blockingBuilder(callable: Try.CheckedConsumer<IN>): OUT {
      val request = inProvider.get()
      Try.run { callable.accept(request) }
      return single0(request).toBlocking().value()
   }

   /**
    * @param callback
    * *
    * @return
    */
   open internal fun single0(callback: Consumer<IN>?): Single<OUT> {
      val request = this.inProvider.get()
      callback?.accept(request)
      return single0(request)
   }

   /**
    * @param request
    * *
    * @return
    */
   open fun local(request: IN): Single<OUT> {
      return create().rx(request)
   }

   /**
    * @param request
    * *
    * @return
    */
   open fun single0(request: IN): Single<OUT> {
      return create().rx(request)
   }

   /**
    * @param request
    * *
    * @return
    */
   open internal fun single0(data: Any?, request: IN): Single<OUT> {
      return create(data, request).rx(request)
   }

   /**
    * @param request
    * *
    * @return
    */
   open internal fun eagerSingle0(request: IN): Single<OUT> {
      return create().rx(request)
   }

   /**
    * @param request
    * *
    * @return
    */
   open internal fun eagerSingle0(data: Any?, request: IN): Single<OUT> {
      return create(data, request).rx(request)
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
