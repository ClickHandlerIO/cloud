package move.action

import com.google.common.base.Throwables
import com.netflix.hystrix.*
import io.vertx.rxjava.core.Vertx
import javaslang.control.Try
import kotlinx.coroutines.experimental.rx1.await
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

    protected open fun init() {
        // Timeout milliseconds.
        var timeoutMillis = DEFAULT_TIMEOUT_MILLIS
        if (actionConfig != null) {
            if (actionConfig.maxExecutionMillis == 0) {
                timeoutMillis = 0
            } else if (actionConfig.maxExecutionMillis > 0) {
                timeoutMillis = actionConfig.maxExecutionMillis
            }
        }

        this.timeoutMillis = timeoutMillis

        // Enable timeout?
        if (timeoutMillis > 0) {
            if (timeoutMillis < 100) {
                // Looks like somebody decided to put seconds instead of milliseconds.
                timeoutMillis = timeoutMillis * 1000
            } else if (timeoutMillis < 1000) {
                timeoutMillis = DEFAULT_TIMEOUT_MILLIS
            }

            this.timeoutMillis = timeoutMillis

            commandPropertiesDefaults.withExecutionTimeoutEnabled(true)
            commandPropertiesDefaults.withExecutionTimeoutInMilliseconds(timeoutMillis)
            this.executionTimeoutEnabled = true
        }

        // Determine Hystrix isolation strategy.
        val hystrixIsolation = HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE

        // Set Hystrix isolation strategy.
        commandPropertiesDefaults.withExecutionIsolationStrategy(hystrixIsolation)
        commandPropertiesDefaults.withFallbackEnabled(true)

        // Build HystrixObservableCommand.Setter default.
        defaultObservableSetter = HystrixObservableCommand.Setter
                // Set Group Key
                .withGroupKey(this.groupKey)
                // Set default command props
                .andCommandPropertiesDefaults(commandPropertiesDefaults)
                // Set command key
                .andCommandKey(this.commandKey)

        if (actionConfig != null && actionConfig.maxConcurrentRequests > 0) {
            maxConcurrentRequests = actionConfig.maxConcurrentRequests
            commandPropertiesDefaults.withExecutionIsolationSemaphoreMaxConcurrentRequests(actionConfig!!.maxConcurrentRequests)
        }
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
                .withExecutionIsolationStrategy(commandPropertiesDefaults.executionIsolationStrategy)
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

    protected fun create(request: IN): A {
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

    /**
     * @return
     */
    open fun execute(callable: Try.CheckedConsumer<IN>): OUT {
        val request = inProvider.get()
        Try.run { callable.accept(request) }
        return waitForResponse(request)
    }

    /**
     * @param request
     * *
     * @return
     */
    @Deprecated("")
    open fun execute(request: IN): OUT {
        try {
            return observe0(create(request)
            ).toBlocking().toFuture().get()
        } catch (e: Throwable) {
            Throwables.throwIfUnchecked(e)
            throw RuntimeException(e)
        }
    }

    open fun executeBlocking(request: IN): OUT {
        try {
            return observe0(create(request)
            ).toBlocking().toFuture().get()
        } catch (e: Throwable) {
            throw RuntimeException(e)
        }
    }

    /**
     * @return
     */
    fun waitForResponse(callable: Try.CheckedConsumer<IN>): OUT {
        val request = inProvider.get()
        Try.run { callable.accept(request) }
        return waitForResponse(request)
    }

    /**
     * @param request
     * *
     * @return
     */
    fun waitForResponse(request: IN): OUT {
        try {
            return observe0(create(request)
            ).toBlocking().toFuture().get()
        } catch (e: Throwable) {
            throw RuntimeException(e)
        }

    }

    /**
     * @param callback
     * *
     * @return
     */
    open fun observe(callback: Consumer<IN>?): Observable<OUT> {
        val request = this.inProvider.get()
        callback?.accept(request)
        return observe(request)
    }

    /**
     * @param request
     * *
     * @return
     */
    open fun single(request: IN): Single<OUT> {
        return observe0(create(request)).toSingle()
    }

    /**
     * @param request
     * *
     * @return
     */
    open fun observe(request: IN): Observable<OUT> {
        return observe0(create(request))
    }

    open suspend fun await(request: IN): OUT {
        return observe(request).toSingle().await()
    }

    /**
     * @param request
     */
    fun fireAndForget(request: IN) {
        observe(request).subscribe()
    }

    /**
     * @param callback
     * *
     * @return
     */
    fun fireAndForget(callback: Consumer<IN>?) {
        val request = this.inProvider.get()
        callback?.accept(request)
        observe(request).subscribe()
    }

    /**
     * @param request\
     */
    protected fun observe0(action: A): Observable<OUT> {
        return Single.create<OUT> { subscriber ->
            // Build observable.
            val observable = action.toObservable()
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
        internal val DEFAULT_TIMEOUT_MILLIS = 5000
        internal val MIN_TIMEOUT_MILLIS = 200

        fun current(): ActionContext {
            return Action.currentContext()
        }
    }
}
