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

    override fun setCommandSetter(setter: HystrixCommand.Setter?) {
        this.setter = setter
    }

    inner class Command(setter: HystrixCommand.Setter?) : HystrixCommand<OUT>(setter) {
        override fun run(): OUT {
            return runBlocking {
                AbstractAction.contextLocal.set(actionContext())

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

                    if (shouldExecuteFallback(this@KBlockingAction.executeException!!, this@KBlockingAction.executeCause!!)) {
                        throw this@KBlockingAction.executeException!!
                    } else {
                        handleException(this@KBlockingAction.executeException!!, this@KBlockingAction.executeCause!!, false)
                    }
                } finally {
                    AbstractAction.contextLocal.remove()
                }
            }
        }

        override fun getFallback(): OUT {
            return runBlocking {
                AbstractAction.contextLocal.set(actionContext())
                try {
                    if (!isFallbackEnabled || !shouldExecuteFallback(this@KBlockingAction.executeException!!, this@KBlockingAction.executeCause!!)) {
                        val e = ActionFallbackException()
                        try {
                            handleException(e, e, true)
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

                    try {
                        handleException(this@KBlockingAction.fallbackException!!, this@KBlockingAction.fallbackCause!!, true)
                    } catch (e2: Throwable) {
                        throw e2
                    }
                } finally {
                    AbstractAction.contextLocal.remove()
                }
            }
        }
    }

    /**
     * @return
     */
    protected fun build(): HystrixCommand<OUT> {
        return Command(getCommandSetter())
    }

    /**
     * @return
     */
    override fun getCommand(): HystrixCommand<OUT> {
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