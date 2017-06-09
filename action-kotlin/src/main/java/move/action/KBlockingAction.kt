package move.action

import com.google.common.base.Throwables
import com.netflix.hystrix.HystrixCommand
import com.netflix.hystrix.exception.HystrixTimeoutException
import kotlinx.coroutines.experimental.runBlocking
import java.util.concurrent.TimeoutException

abstract class KBlockingAction<IN, OUT> : BaseBlockingAction<IN, OUT>() {
    private var command: HystrixCommand<OUT>? = null
    private var setter: HystrixCommand.Setter? = null
    private var ctx: io.vertx.rxjava.core.Context? = null
    private var executeException: Throwable? = null
    private var executeCause: Throwable? = null
    private var fallbackException: Throwable? = null
    private var fallbackCause: Throwable? = null

    protected fun getCommandSetter(): HystrixCommand.Setter? {
        return setter
    }

    override fun configureCommand(setter: HystrixCommand.Setter?) {
        this.setter = setter
    }

    /**
     *
     */
    protected inline fun reply(block: OUT.() -> Unit): OUT = replyProvider().get().apply(block)

    inner class Command(setter: HystrixCommand.Setter?) : HystrixCommand<OUT>(setter) {
        override fun run(): OUT {
            val actionContext = actionContext()
            val result = runBlocking {
                Action.contextLocal.set(actionContext)
                try {
                    this@KBlockingAction.execute(request)
                } catch (e: Throwable) {
                    this@KBlockingAction.executeException = let {
                        if (this@Command.executionException == null)
                            e
                        else
                            this@Command.executionException
                    }

                    this@KBlockingAction.executeCause = Throwables.getRootCause(this@KBlockingAction.executeException)
                    if (isActionTimeout(this@KBlockingAction.executeCause!!)) {
                        throw this@KBlockingAction.executeCause!!
                    }

                    if (isFallbackEnabled && shouldExecuteFallback(this@KBlockingAction.executeException!!, this@KBlockingAction.executeCause!!)) {
                        throw this@KBlockingAction.executeException!!
                    } else {
                        recover(this@KBlockingAction.executeException!!, this@KBlockingAction.executeCause!!, false)
                    }
                } finally {
                    Action.contextLocal.remove()
                }
            }

            return result
        }

        override fun getFallback(): OUT {
            val actionContext = actionContext()
            return runBlocking {
                Action.contextLocal.set(actionContext)
                try {
                    if (!isFallbackEnabled || !shouldExecuteFallback(this@KBlockingAction.executeException!!, this@KBlockingAction.executeCause!!)) {
                        val e = ActionFallbackException()
                        try {
                            recover(e, e, true)
                        } catch (e: Exception) {
                            throw e
                        }
                    } else {
                        executeFallback(request, this@KBlockingAction.executeException, this@KBlockingAction.executeCause)
                    }
                } catch (e: Throwable) {
                    this@KBlockingAction.fallbackException = let {
                        if (this@Command.executionException == null)
                            e
                        else
                            this@Command.executionException
                    }

                    this@KBlockingAction.fallbackCause = Throwables.getRootCause(this@KBlockingAction.fallbackException)
                    if (isActionTimeout(this@KBlockingAction.fallbackCause!!)) {
                        throw this@KBlockingAction.fallbackCause!!
                    }

                    try {
                        recover(this@KBlockingAction.fallbackException!!, this@KBlockingAction.fallbackCause!!, true)
                    } catch (e2: Throwable) {
                        throw e2
                    }
                } finally {
                    Action.contextLocal.remove()
                }
            }
        }
    }

    /**
     * @return
     */
    protected fun build(): HystrixCommand<OUT> {
        return Command(setter)
    }

    /**
     * @return
     */
    override fun command(): HystrixCommand<OUT> {
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