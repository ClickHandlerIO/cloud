package move.action

import com.google.common.base.Throwables
import com.netflix.hystrix.HystrixObservableCommand
import com.netflix.hystrix.exception.HystrixRuntimeException
import com.netflix.hystrix.exception.HystrixTimeoutException
import com.netflix.hystrix.isTimedOut
import io.vertx.rxjava.core.WorkerExecutor
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.rx1.await
import kotlinx.coroutines.experimental.rx1.rxSingle
import rx.Observable
import rx.Single
import rx.SingleSubscriber
import rx.Subscription
import java.util.concurrent.TimeoutException
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.startCoroutine

abstract class BaseAction<IN, OUT> : BaseAsyncAction<IN, OUT>() {
    private var command: HystrixObservableCommand<OUT>? = null
    private var setter: HystrixObservableCommand.Setter? = null
    private var ctx: io.vertx.rxjava.core.Context? = null
    private var timedOut: Boolean = false
    private var executeException: Throwable? = null
    private var executeCause: Throwable? = null
    private var fallbackException: Throwable? = null
    private var fallbackCause: Throwable? = null

    private var dispatcher: CoroutineDispatcher? = null

    protected fun dispatcher(): CoroutineDispatcher = dispatcher!!

    protected fun commandSetter(): HystrixObservableCommand.Setter? {
        return setter
    }

    override fun configureCommand(setter: HystrixObservableCommand.Setter?) {
        this.setter = setter
    }

    /**
     *
     */
    protected inline fun reply(block: OUT.() -> Unit): OUT = replyProvider().get().apply(block)

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
                // HystrixTimer calls unsubscribe right before onError() which will
                // call "cancel()" on the coroutine which will throw a Job Cancelled exception
                // instead of a HystrixTimeoutException. Flag as timedOut if the HystrixCommand
                // actually timed out.
                if (!timedOut)
                    timedOut = isTimedOut()

                // Always cancel coroutine.
                cancel()
            }
        }

        override fun construct(): Observable<OUT>? {
            val vertx = vertx()
            ctx = vertx.orCreateContext
            dispatcher = VertxContextDispatcher(actionContext(), ctx!!)

            return hystrixSingle(dispatcher!!) {
                try {
                    execute(request)
                } catch (e: Throwable) {
                    this@BaseAction.executeException = let {
                        if (this@Command.executionException == null)
                            e
                        else
                            this@Command.executionException
                    }

                    this@BaseAction.executeCause = Throwables.getRootCause(this@BaseAction.executeException)

                    if (isActionTimeout(this@BaseAction.executeCause!!)) {
                        throw this@BaseAction.executeCause!!
                    }

                    if (isFallbackEnabled && shouldExecuteFallback(this@BaseAction.executeException!!, this@BaseAction.executeCause!!)) {
                        throw this@BaseAction.executeException!!
                    } else {
                        recover(this@BaseAction.executeException!!, this@BaseAction.executeCause!!, false)
                    }
                }
            }.toObservable()
        }

        override fun resumeWithFallback(): Observable<OUT> {
            return hystrixSingle(dispatcher!!) {
                try {
                    if (!isFallbackEnabled || !shouldExecuteFallback(this@BaseAction.executeException!!, this@BaseAction.executeCause!!)) {
                        val e = ActionFallbackException()
                        try {
                            recover(e, e, true)
                        } catch (e: Exception) {
                            throw e
                        }
                    } else {
                        executeFallback(request, this@BaseAction.executeException, this@BaseAction.executeCause)
                    }
                } catch (e: Throwable) {
                    this@BaseAction.fallbackException = let {
                        if (this@Command.executionException == null)
                            e
                        else
                            this@Command.executionException
                    }

                    this@BaseAction.fallbackCause = Throwables.getRootCause(this@BaseAction.fallbackException)

                    if (isActionTimeout(this@BaseAction.fallbackCause!!)) {
                        throw this@BaseAction.fallbackCause!!
                    }

                    try {
                        recover(this@BaseAction.fallbackException!!, this@BaseAction.fallbackCause!!, true)
                    } catch (e2: Throwable) {
                        if (e2 is HystrixRuntimeException && e2.cause != null)
                            throw e2.cause!!
                        throw e2
                    }
                }
            }.toObservable()
        }
    }

    protected fun build(): HystrixObservableCommand<OUT> {
        return Command(setter)
    }

    /**
     * @return
     */
    override fun command(): HystrixObservableCommand<OUT> {
        if (command != null) {
            return command!!
        }
        command = build()
        return command!!
    }

    /**
     * Runs a block on the default vertx WorkerExecutor in a coroutine.
     */
    suspend fun <T> blocking(block: suspend () -> T): T = blocking(false, block)

    /**
     * Runs a block on the default vertx WorkerExecutor in a coroutine.
     */
    suspend fun <T> blocking(ordered: Boolean, block: suspend () -> T): T =
            vertx().rxExecuteBlocking<T>({
                try {
                    it.complete(runBlocking { block() })
                } catch (e: Throwable) {
                    it.fail(e)
                }
            }, ordered).await()

    /**
     * Runs a block on a specified vertx WorkerExecutor in a coroutine.
     */
    suspend fun <T> blocking(executor: WorkerExecutor, block: suspend () -> T): T =
            executor.rxExecuteBlocking<T> {
                try {
                    it.complete(runBlocking { block() })
                } catch (e: Throwable) {
                    it.fail(e)
                }
            }.await()

    /**
     * Runs a block on a specified vertx WorkerExecutor in a coroutine.
     */
    suspend fun <T> blocking(executor: WorkerExecutor, ordered: Boolean, block: suspend () -> T): T =
            executor.rxExecuteBlocking<T>({
                try {
                    it.complete(runBlocking { block() })
                } catch (e: Throwable) {
                    it.fail(e)
                }
            }, ordered).await()

    /**
     * Runs a block on the default vertx WorkerExecutor in a coroutine.
     */
    suspend fun <T> worker(block: suspend () -> T): Single<T> = worker(false, block)

    /**
     * Runs a block on the default vertx WorkerExecutor in a coroutine.
     */
    suspend fun <T> worker(ordered: Boolean, block: suspend () -> T): Single<T> =
            vertx().rxExecuteBlocking<T>({
                try {
                    it.complete(runBlocking { block() })
                } catch (e: Throwable) {
                    it.fail(e)
                }
            }, ordered)

    /**
     * Runs a block on a specified vertx WorkerExecutor in a coroutine.
     */
    suspend fun <T> worker(executor: WorkerExecutor, block: suspend () -> T): Single<T> = worker(executor, false, block)

    /**
     * Runs a block on a specified vertx WorkerExecutor in a coroutine.
     */
    suspend fun <T> worker(executor: WorkerExecutor, ordered: Boolean, block: suspend () -> T): Single<T> =
            executor.rxExecuteBlocking<T>({
                try {
                    it.complete(runBlocking { block() })
                } catch (e: Throwable) {
                    it.fail(e)
                }
            }, ordered)

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
    protected fun <T> single(block: suspend CoroutineScope.() -> T): Single<T> = rxSingle(dispatcher(), block)

    /**
     * @param request
     */
    protected abstract suspend fun execute(request: IN): OUT

    /**
     *
     */
    protected abstract suspend fun recover(caught: Throwable, cause: Throwable, isFallback: Boolean): OUT

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