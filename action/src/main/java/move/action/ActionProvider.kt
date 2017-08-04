package move.action

import com.google.common.base.Throwables
import com.netflix.hystrix.*
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

   private val commandPropertiesDefaults = HystrixCommandProperties.Setter()
   private val threadPoolPropertiesDefaults = HystrixThreadPoolProperties.Setter()

   private var defaultSetter: HystrixCommand.Setter? = null
   private var defaultObservableSetter: HystrixObservableCommand.Setter? = null
   private var executionTimeoutEnabled: Boolean = false
   private var timeoutMillis: Int = 0
   var maxConcurrentRequests: Int = 0
      internal set

   val groupKey: HystrixCommandGroupKey = HystrixCommandGroupKey.Factory.asKey(actionConfig?.groupKey ?: "")
   //    val threadPoolKey: HystrixThreadPoolKey = HystrixThreadPoolKey.Factory.asKey(actionConfig?.groupKey ?: "")
   val commandKey: HystrixCommandKey = HystrixCommandKey.Factory.asKey(commandKey(actionConfig, actionClass))

   var isExecutionTimeoutEnabled: Boolean
      get() = executionTimeoutEnabled
      set(enabled) {
         commandPropertiesDefaults.withExecutionTimeoutEnabled(enabled)

         if (defaultObservableSetter != null) {
            defaultObservableSetter!!.andCommandPropertiesDefaults(commandPropertiesDefaults)
         } else if (defaultSetter != null) {
            defaultSetter!!.andCommandPropertiesDefaults(commandPropertiesDefaults)
         }
      }

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

   open fun parallelism(): Int {
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
      } else if (actionConfig.maxConcurrentRequests == ActionConfig.DEFAULT_PARALLELISM) {
         val p = actionConfig.parallelism
         if (p == ActionConfig.DEFAULT_PARALLELISM) {
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
      } else {
         val p = actionConfig.parallelism
         if (p == ActionConfig.DEFAULT_PARALLELISM) {
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
         if (actionConfig.maxExecutionMillis == 0) {
            timeoutMillis = if (actionConfig.timeoutMillis != ActionConfig.DEFAULT_TIMEOUT_MILLIS) {
               actionConfig.timeoutMillis
            } else {
               ActionConfig.DEFAULT_TIMEOUT_MILLIS
            }
         } else if (actionConfig.maxExecutionMillis > 0) {
            timeoutMillis = if (actionConfig.maxExecutionMillis != ActionConfig.DEFAULT_TIMEOUT_MILLIS) {
               actionConfig.maxExecutionMillis
            } else {
               actionConfig.timeoutMillis
            }
         }
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

         commandPropertiesDefaults.withExecutionTimeoutEnabled(true)
         commandPropertiesDefaults.withExecutionTimeoutInMilliseconds(timeoutMillis)
         executionTimeoutEnabled = true
      }

      // Set Hystrix isolation strategy to SEMAPHORE. We don't use hystrix for thread pooling.
      commandPropertiesDefaults.withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE)
      commandPropertiesDefaults.withFallbackEnabled(true)

      // Build HystrixObservableCommand.Setter default.
      defaultObservableSetter = HystrixObservableCommand.Setter
         // Set Group Key
         .withGroupKey(this.groupKey)
         // Set default command props
         .andCommandPropertiesDefaults(commandPropertiesDefaults)
         // Set command key
         .andCommandKey(this.commandKey)


      commandPropertiesDefaults.withExecutionIsolationSemaphoreMaxConcurrentRequests(parallelism())
      commandPropertiesDefaults.withExecutionIsolationThreadInterruptOnFutureCancel(true)
   }

   protected fun commandKey(config: ActionConfig?, actionClass: Class<*>): String {
      if (config != null) {
         if (config.commandKey.isEmpty()) {
            return actionClass.name
         } else {
            return config.commandKey
         }
      }

      return actionClass.name
   }

   protected fun configureCommand(action: A, context: ActionContext): HystrixObservableCommand.Setter {
      // Calculate max execution millis.
      var maxMillis = timeoutMillis.toLong()
      val now = System.currentTimeMillis()
      if (now + maxMillis > context.timesOutAt) {
         maxMillis = context.timesOutAt - now
      }
      if (maxMillis < MIN_TIMEOUT_MILLIS) {
         maxMillis = MIN_TIMEOUT_MILLIS.toLong()
      }

      // Clone command properties from default and adjust the timeout.
      val commandProperties = HystrixCommandProperties.Setter()
         .withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE)
         .withExecutionTimeoutEnabled(true)
         .withExecutionTimeoutInMilliseconds(maxMillis.toInt())
         .withFallbackEnabled(action.isFallbackEnabled)
         .withExecutionIsolationThreadInterruptOnFutureCancel(true)
         .withExecutionIsolationThreadInterruptOnTimeout(true)
         .withRequestCacheEnabled(false)
         .withRequestLogEnabled(false)

      if (commandPropertiesDefaults.executionIsolationSemaphoreMaxConcurrentRequests != null) {
         commandProperties.withExecutionIsolationSemaphoreMaxConcurrentRequests(commandPropertiesDefaults.executionIsolationSemaphoreMaxConcurrentRequests!!)
         commandProperties.withFallbackIsolationSemaphoreMaxConcurrentRequests(commandPropertiesDefaults.executionIsolationSemaphoreMaxConcurrentRequests!!)
      }

      return HystrixObservableCommand.Setter
         .withGroupKey(groupKey)
         .andCommandKey(commandKey)
         .andCommandPropertiesDefaults(commandProperties)
   }

   internal fun create(request: IN): A {
      // Create new Action instance.
      val action = actionProvider.get()

      // Get or create ActionContext.
      var context: ActionContext? = Action.contextLocal.get()
      if (context == null) {
         context = ActionContext(timeoutMillis.toLong(), this, io.vertx.core.Vertx.currentContext())
      }

      action.init(
         vertx,
         context,
         request,
         inProvider,
         outProvider,
         // Set the command setter.
         configureCommand(action, context)
      )

      // Return action instance.
      return action
   }

   internal fun create(data: Any?, request: IN): A {
      // Create new Action instance.
      val action = actionProvider.get()

      // Get or create ActionContext.
      var context: ActionContext? = Action.contextLocal.get()
      if (context == null) {
         context = ActionContext(timeoutMillis.toLong(), this, io.vertx.core.Vertx.currentContext())
      }
      context.data = data

      action.init(
         vertx,
         context,
         request,
         inProvider,
         outProvider,
         // Set the command setter.
         configureCommand(action, context)
      )

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
      return observe0(create(request)).toSingle()
   }

   /**
    * @param request
    * *
    * @return
    */
   open internal fun single0(request: IN): Single<OUT> {
      return observe0(create(request)).toSingle()
   }

   /**
    * @param request
    * *
    * @return
    */
   open internal fun single0(data: Any?, request: IN): Single<OUT> {
      return observe0(create(data, request)).toSingle()
   }

   /**
    * @param request
    * *
    * @return
    */
   open internal fun eagerSingle0(request: IN): Single<OUT> {
      return observe0(true, create(request)).toSingle()
   }

   /**
    * @param request
    * *
    * @return
    */
   open internal fun eagerSingle0(data: Any?, request: IN): Single<OUT> {
      return observe0(true, create(data, request)).toSingle()
   }

   protected fun observe0(action: A): Observable<OUT> {
      return observe0(false, action)
   }

   /**
    * @param request\
    */
   protected fun observe0(eager: Boolean, action: A): Observable<OUT> {
      return Single.create<OUT> { subscriber ->
         // Build observable.
         val observable = if (eager) action.observe() else action.toObservable()
         val ctx = io.vertx.core.Vertx.currentContext()
         val actionContext = action.context

         observable.subscribe(
            { r ->
               if (!subscriber.isUnsubscribed) {
                  if (ctx != null && Vertx.currentContext() !== ctx) {
                     ctx.runOnContext { a ->
                        if (!subscriber.isUnsubscribed) {
                           Action.contextLocal.set(actionContext)
                           try {
                              subscriber.onSuccess(r)
                           } finally {
                              Action.contextLocal.remove()
                           }
                        }
                     }
                  } else {
                     Action.contextLocal.set(actionContext)
                     try {
                        subscriber.onSuccess(r)
                     } finally {
                        Action.contextLocal.remove()
                     }
                  }
               }
            }
         ) { e ->
            if (!subscriber.isUnsubscribed) {
               if (ctx != null && Vertx.currentContext() !== ctx) {
                  ctx.runOnContext { a ->
                     if (!subscriber.isUnsubscribed) {
                        Action.contextLocal.set(actionContext)
                        try {
                           subscriber.onError(e)
                        } finally {
                           Action.contextLocal.remove()
                        }
                     }
                  }
               } else {
                  Action.contextLocal.set(actionContext)
                  try {
                     subscriber.onError(e)
                  } finally {
                     Action.contextLocal.remove()
                  }
               }
            }
         }
      }.toObservable()
   }

   companion object {
      internal val MIN_TIMEOUT_MILLIS = 200
      internal val MILLIS_TO_SECONDS_THRESHOLD = 1000
      internal val DEFAULT_CONCURRENCY_INTERNAL = 10000
      internal val DEFAULT_CONCURRENCY_REMOTE = 10000
      internal val DEFAULT_CONCURRENCY_WORKER = 10000
      internal val DEFAULT_CONCURRENCY_SCHEDULED = 1

      fun current(): ActionContext? {
         return Action.currentContext()
      }
   }
}
