package move.action

import com.google.common.base.Throwables
import com.netflix.hystrix.HystrixObservableCommand
import com.netflix.hystrix.exception.HystrixTimeoutException
import com.netflix.hystrix.isTimedOut
import io.vertx.rxjava.core.Vertx
import kotlinx.coroutines.experimental.*
import rx.Observable
import rx.Single
import rx.SingleSubscriber
import rx.Subscription
import java.util.concurrent.TimeoutException
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.startCoroutine

abstract class KAction<IN, OUT> : BaseObservableAction<IN, OUT>() {
    private var command: HystrixObservableCommand<OUT>? = null
    private var setter: HystrixObservableCommand.Setter? = null
    private var ctx: io.vertx.rxjava.core.Context? = null
    private var timedOut: Boolean = false
    private var executeException: Throwable? = null
    private var executeCause: Throwable? = null
    private var fallbackException: Throwable? = null
    private var fallbackCause: Throwable? = null

    protected fun getCommandSetter(): HystrixObservableCommand.Setter? {
        return setter
    }

    override fun setCommandSetter(setter: HystrixObservableCommand.Setter?) {
        this.setter = setter
    }

    inner class Command(setter: HystrixObservableCommand.Setter?) : HystrixObservableCommand<OUT>(setter) {
        private var timedOut: Boolean = false

        /**
         * Creates cold [Single] that runs a given [block] in a coroutine.
         * Every time the returned single is subscribed, it starts a new coroutine in the specified [context].
         * Coroutine returns a single value. Unsubscribing cancels running coroutine.
         *
         * | **Coroutine action**                  | **Signal to subscriber**
         * | ------------------------------------- | ------------------------
         * | Returns a value                       | `onSuccess`
         * | Failure with exception or unsubscribe | `onError`
         */
        fun <T> hystrixSingle(
                context: CoroutineContext,
                block: suspend CoroutineScope.() -> T
        ): Single<T> = Single.create<T> { subscriber ->
            val newContext = newCoroutineContext(context)
            val coroutine = HystrixSingleCoroutine(newContext, subscriber)
            coroutine.initParentJob(context[Job])
            subscriber.add(coroutine)
            block.startCoroutine(coroutine, coroutine)
        }

        inner class HystrixSingleCoroutine<T>(
                override val parentContext: CoroutineContext,
                private val subscriber: SingleSubscriber<T>
        ) : AbstractCoroutine<T>(true), Subscription {
            @Suppress("UNCHECKED_CAST")
            override fun afterCompletion(state: Any?, mode: Int) {
                if (state is CompletedExceptionally)
                    subscriber.onError(let {
                        if (timedOut)
                            HystrixTimeoutException()
                        else
                            state.exception
                    })
                else
                    subscriber.onSuccess(state as T)
            }

            override fun isUnsubscribed(): Boolean = isCompleted

            override fun unsubscribe() {
                if (!timedOut)
                    timedOut = isTimedOut()

                cancel()
            }
        }

        override fun construct(): Observable<OUT>? {
            ctx = Vertx.currentContext()
            val pool: CoroutineDispatcher = let {
                if (ctx == null)
                    Unconfined
                else
                    VertxContextDispatcher(ctx!!)
            }

            return hystrixSingle(pool) {
                AbstractAction.contextLocal.set(actionContext())
                try {
                    execute(request)
                } catch (e: Throwable) {
                    this@KAction.executeException = let {
                        if (this@Command.executionException == null)
                            e
                        else
                            this@Command.executionException
                    }

                    this@KAction.executeCause = Throwables.getRootCause(this@KAction.executeException)

                    if (shouldExecuteFallback(this@KAction.executeException!!, this@KAction.executeCause!!)) {
                        throw this@KAction.executeException!!
                    } else {
                        handleException(this@KAction.executeException!!, this@KAction.executeCause!!, false)
                    }
                } finally {
                    AbstractAction.contextLocal.remove()
                }
            }.toObservable()
        }

        override fun resumeWithFallback(): Observable<OUT> {
            val pool: CoroutineDispatcher = let {
                if (ctx == null)
                    Unconfined
                else
                    VertxContextDispatcher(ctx!!)
            }

            return hystrixSingle(pool) {
                AbstractAction.contextLocal.set(actionContext())
                try {
                    if (!isFallbackEnabled || !shouldExecuteFallback(this@KAction.executeException!!, this@KAction.executeCause!!)) {
                        val e = ActionFallbackException()
                        try {
                            handleException(e, e, true)
                        } catch (e: Exception) {
                            throw e
                        }
                    } else {
                        executeFallback(request, this@KAction.executeException, this@KAction.executeCause)
                    }
                } catch (e: Throwable) {
                    this@KAction.fallbackException = let {
                        if (this@Command.executionException == null)
                            e
                        else
                            this@Command.executionException
                    }

                    this@KAction.fallbackCause = Throwables.getRootCause(this@KAction.fallbackException)

                    try {
                        handleException(this@KAction.fallbackException!!, this@KAction.fallbackCause!!, true)
                    } catch (e2: Throwable) {
                        throw e2
                    }
                } finally {
                    AbstractAction.contextLocal.remove()
                }
            }.toObservable()
        }
    }

    protected fun build(): HystrixObservableCommand<OUT> {
        return Command(getCommandSetter())
    }

    /**
     * @return
     */
    override fun getCommand(): HystrixObservableCommand<OUT> {
        if (command != null) {
            return command!!
        }
        command = build()
        return command!!
    }

    /**
     * @param request
     */
    protected abstract suspend fun execute(request: IN): OUT

    /**
     *
     */
    protected abstract suspend fun handleException(caught: Throwable, cause: Throwable, isFallback: Boolean): OUT

    /**
     *
     */
    protected suspend open fun shouldExecuteFallback(caught: Throwable, cause: Throwable): Boolean {
        return when (cause) {
            is HystrixTimeoutException -> false
            is TimeoutException -> false
            else -> true
        }
    }

    /**
     * @param request
     */
    protected suspend open fun executeFallback(request: IN, caught: Throwable?, cause: Throwable?): OUT {
        return execute(request)
    }
}