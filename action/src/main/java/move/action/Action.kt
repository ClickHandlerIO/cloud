package move.action

import com.google.common.base.Throwables
import io.vertx.circuitbreaker.CircuitBreakerState
import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.core.WorkerExecutor
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.rx1.await
import kotlinx.coroutines.experimental.rx1.rxSingle
import kotlinx.coroutines.experimental.selects.SelectBuilder
import kotlinx.coroutines.experimental.selects.select
import rx.Single
import rx.SingleSubscriber
import rx.Subscription
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.startCoroutine
import kotlin.coroutines.experimental.suspendCoroutine
import kotlin.reflect.KClass

/**
 * @author Clay Molocznik
 */
abstract class Action<IN : Any, OUT : Any> internal constructor() : IAction<IN, OUT>() {
   // Request
   var _request: IN? = null
   val request: IN
      get() = _request!!

   // Breaker
   lateinit var provider: ActionProvider<Action<IN, OUT>, IN, OUT>
   private lateinit var breakerOperation: ActionCircuitBreakerMetrics.Operation

   // Assigned EventLoop
   private lateinit var eventLoop: ActionEventLoopContext

   // Coroutine Fields
   private lateinit var dispatcher: ActionContextDispatcher
   private lateinit var coroutineBlock: suspend CoroutineScope.() -> OUT
   internal lateinit var coroutine: ActionJobCoroutine
      get

   // Action Context
   val context: ActionContext
      get() = dispatcher.actionContext

   // Rx
   internal lateinit var single: Single<OUT>
   private lateinit var subscriber: SingleSubscriber<in OUT>

   // Local fields
   private var timedOut: Boolean = false
   private var timesOutAt: Long = 0L
   private var reason: Throwable? = null
   private var cause: Throwable? = null

   // Fallback
   private var retryCount = 0
   open val maxRetries
      get() = 0

   // ID
   private var actionId = 0L

   /**
    * Flag to enable/disable fallback processing.
    * Use "shouldFallback()" method for more granular use cases.
    */
   open val isFallbackEnabled: Boolean
      get() = false

   // TODO: Track dependent subscriptions that need to be cleaned automatically

   internal fun init(provider: ActionProvider<Action<IN, OUT>, IN, OUT>,
                     context: ActionContext,
                     timesOutAt: Long) {
      this.eventLoop = context.eventLoop
      this.provider = provider
      val breaker = provider.breaker
      this.breakerOperation = breaker.metrics.enqueue()
      this.dispatcher = ActionContextDispatcher(context, context.eventLoop)

      val newContext = newCoroutineContext(dispatcher)
      this.timesOutAt = timesOutAt
      this.coroutine = ActionJobCoroutine(newContext)
      this.coroutine.initParentJob(dispatcher[Job])
      this.coroutineBlock = {
         var rep: OUT? = null
         var state = breaker.state()

         // Begin operation
         breakerOperation.begin()

         actionId =
            if (timesOutAt > 0L)
               eventLoop.registerAction(this@Action, timesOutAt)
            else
               eventLoop.registerAction(this@Action)

         try {
            onStart()

            // Handle OPEN state
            if (state == CircuitBreakerState.OPEN) {
               throw CircuitOpenException()
            }

            // Handle HALF_OPEN state
            if (state == CircuitBreakerState.HALF_OPEN && breaker.passed.incrementAndGet() != 1) {
               throw CircuitOpenException()
            }

            rep = execute()
            breakerOperation.complete()
            breaker.reset()
            rep
         } catch (e: Throwable) {
            reason = e
            cause = Throwables.getRootCause(e)

            if (e is CircuitOpenException) {
               breakerOperation.shortCircuited()

               try {
                  if (isFallbackEnabled && shouldFallback(e, cause!!)) {
                     rep = executeFallback(reason, cause)
                     breakerOperation.fallbackSucceed()
                     rep
                  } else {
                     breakerOperation.fallbackFailed()
                     throw e
                  }
               } catch (e2: Throwable) {
                  throw e2
               }
            } else {
               breaker.incrementFailures()
               breakerOperation.failed()

               // Ensure fallback is enabled.
               if (isFallbackEnabled && shouldFallback(e, cause!!)) {
                  loop@
                  for (i in 0..maxRetries) {
                     retryCount++

                     if (retryCount <= maxRetries) {
                        try {
                           rep = executeFallback(reason, cause)
                           breakerOperation.fallbackSucceed()

                           break@loop
                        } catch (e2: Throwable) {
                           breaker.incrementFailures()
                           breakerOperation.fallbackFailed()
                           reason = e2
                           cause = Throwables.getRootCause(e2)
                        }
                     } else {
                        breakerOperation.failed()
                        throw e
                     }
                  }

                  // Ensure Reply is not null
                  if (rep == null)
                     throw NullPointerException("Reply cannot be null")
                  else
                     rep
               } else {
                  throw e
               }
            }
         } finally {
            if (timesOutAt > 0L)
               eventLoop.removeTimeOutAction(actionId)
            else
               eventLoop.removeAction(actionId)
         }
      }

      single = Single.create {
         subscriber = it
         // Bind coroutine to subscriber.
         it.add(coroutine)
         start()
      }

      afterInit()
   }

   /**
    *
    */
   fun isTimeOut(time: Long) = time >= timesOutAt

   /**
    *
    */
   internal open fun afterInit() {
   }

   /**
    *
    */
   private fun start() {
      coroutineBlock.startCoroutine(coroutine, coroutine)
   }

   internal open suspend fun onStart() {
   }

   fun timedOut() {
      if (coroutine.isActive) {
         timedOut = true
         coroutine.cancel(CancellationException())
      }
   }

   /**
    * Helper function for other languages such as Java to suspend
    * the action and resume later.
    */
   protected suspend fun suspend(block: (Continuation<OUT>) -> Unit): OUT {
      return suspendCoroutine {
         block(it)
      }
   }

   /**
    *
    */
   protected suspend open fun recover(caught: Throwable, cause: Throwable, isFallback: Boolean): OUT {
      throw cause
   }

   /**
    * @param request
    */
   protected abstract suspend fun execute(): OUT

   /**
    *
    */
   suspend open fun afterExecute(reply: OUT): OUT {
      // Help GC.
      _request = null
      return reply
   }

   /**
    *
    */
   protected suspend open fun shouldExecuteFallback(caught: Throwable, cause: Throwable): Boolean {
      return when (cause) {
         is ActionTimeoutException -> false
         is TimeoutException -> false
         else -> true
      }
   }

   /**
    *
    */
   protected suspend open fun shouldFallback(caught: Throwable, cause: Throwable): Boolean {
      return shouldExecuteFallback(caught, cause)
   }

   /**
    * @param request
    */
   protected suspend open fun executeFallback(caught: Throwable?, cause: Throwable?): OUT {
      // Default to running execute() again.
      return execute()
   }

   /**
    * Invoked when Action is executed in the context of "replaying" or "streaming"
    * a queue file. This could also be in real-time and a way to "stream"
    * to other systems.
    */
   open fun replay() {}

   /**
    *
    */
   internal open fun defer(block: suspend () -> Unit) {

   }

   /**
    *
    */
   internal open fun rx(request: IN): Single<OUT> {
      if (_request != null) {
         // Create a new instance.
         return provider.create().rx(request)
      }

      _request = request
      return single
   }

   /**
    *
    */
   internal open suspend operator fun invoke(request: IN): OUT {
      if (_request != null) {
         return provider.create().invoke(request)
      }

      _request = request
      return single.await()
   }

   /**
    *
    */
   internal open suspend infix fun await(request: IN): OUT {
      if (_request != null) {
         return provider.create().await(request)
      }

      _request = request
      return single.await()
   }

   /**
    * Runs a coroutineBlock on the default vertx WorkerExecutor in a coroutine.
    */
   protected suspend fun <T> blocking(block: suspend () -> T): T = blocking(false, block)

   /**
    * Runs a coroutineBlock on the default vertx WorkerExecutor in a coroutine.
    */
   protected suspend fun <T> blocking(ordered: Boolean, block: suspend () -> T): T =
      provider.vertx.rxExecuteBlocking<T>({
         contextLocal.set(context)
         try {
            it.complete(runBlocking { block() })
         } catch (e: Throwable) {
            it.fail(e)
         } finally {
            contextLocal.remove()
         }
      }, ordered).await()

   /**
    * Runs a coroutineBlock on a specified vertx WorkerExecutor in a coroutine.
    */
   protected suspend fun <T> blocking(executor: WorkerExecutor, block: suspend () -> T): T =
      executor.rxExecuteBlocking<T> {
         contextLocal.set(context)
         try {
            it.complete(runBlocking { block() })
         } catch (e: Throwable) {
            it.fail(e)
         } finally {
            contextLocal.remove()
         }
      }.await()

   /**
    * Runs a coroutineBlock on a specified vertx WorkerExecutor in a coroutine.
    */
   suspend fun <T> blocking(executor: WorkerExecutor, ordered: Boolean, block: suspend () -> T): T =
      executor.rxExecuteBlocking<T>({
         contextLocal.set(context)
         try {
            it.complete(runBlocking { block() })
         } catch (e: Throwable) {
            it.fail(e)
         } finally {
            contextLocal.remove()
         }
      }, ordered).await()

   /**
    * Runs a coroutineBlock on the default vertx WorkerExecutor in a coroutine.
    */
   protected suspend fun <T> rxBlocking(block: suspend () -> T): Single<T> = rxBlocking(false, block)

   /**
    * Runs a coroutineBlock on the default vertx WorkerExecutor in a coroutine.
    */
   suspend fun <T> rxBlocking(ordered: Boolean, block: suspend () -> T): Single<T> =
      provider.vertx.rxExecuteBlocking<T>({
         contextLocal.set(context)
         try {
            it.complete(runBlocking { block() })
         } catch (e: Throwable) {
            it.fail(e)
         } finally {
            contextLocal.remove()
         }
      }, ordered)

   /**
    * Runs a coroutineBlock on a specified vertx WorkerExecutor in a coroutine.
    */
   protected suspend fun <T> rxBlocking(executor: WorkerExecutor, block: suspend () -> T): Single<T> =
      rxBlocking(executor, false, block)

   /**
    * Runs a coroutineBlock on a specified vertx WorkerExecutor in a coroutine.
    */
   protected suspend fun <T> rxBlocking(executor: WorkerExecutor, ordered: Boolean, block: suspend () -> T): Single<T> =
      executor.rxExecuteBlocking<T>({
         contextLocal.set(context)
         try {
            it.complete(runBlocking { block() })
         } catch (e: Throwable) {
            it.fail(e)
         } finally {
            contextLocal.remove()
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
   protected fun <T> rx(block: suspend CoroutineScope.() -> T): Single<T> = rxSingle(dispatcher, block)

   /**
    * Delays coroutine for a given time without blocking a thread and resumes it after a specified time.
    * This suspending function is cancellable.
    * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this function
    * immediately resumes with [CancellationException].
    *
    * Note, that delay can be used in [select] invocation with [onTimeout][SelectBuilder.onTimeout] clause.
    *
    * This function delegates to [Delay.scheduleResumeAfterDelay] if the context [CoroutineDispatcher]
    * implements [Delay] interface, otherwise it resumes using a built-in single-threaded scheduled executor service.
    */
   suspend fun delay(time: Long, unit: TimeUnit = TimeUnit.MILLISECONDS) {
      return kotlinx.coroutines.experimental.delay(time, unit)
   }

   /**
    * Synonym of delay
    */
   suspend fun sleep(time: Long, unit: TimeUnit = TimeUnit.MILLISECONDS) {
      return delay(time, unit)
   }

   @Suppress("UNCHECKED_CAST")
   companion object {
      val contextLocal = ThreadLocal<ActionContext?>()

      fun currentContext(): ActionContext? {
         return contextLocal.get()
      }

      val all
         get() = Collections.unmodifiableMap(ActionManager.actionMap)

      inline fun <reified A : Action<IN, OUT>, IN : Any, OUT : Any> providerOf(): ActionProvider<A, IN, OUT>? {
         return ActionManager.actionMap[A::class.java] as ActionProvider<A, IN, OUT>
      }

      /**
       *
       */
      inline infix fun <A : Action<*, *>> of(cls: KClass<A>): A {
         val a = ActionManager.actionMap[cls.java]
         if (a == null) {
            throw RuntimeException()
         }

         return (a as ActionProvider<A, *, *>).create()
      }

      fun <A : Action<*, *>> of(cls: Class<A>): A {
         val a = ActionManager.actionMap[cls]
         if (a == null) {
            throw RuntimeException()
         }

         return (a as ActionProvider<A, *, *>).create()
      }

      inline fun <reified A : Action<*, *>> of(): A {
         val a = ActionManager.actionMap[A::class.java]
         if (a == null) {
            throw RuntimeException()
         }

         return (a as ActionProvider<A, *, *>).create()
      }

      suspend inline fun <reified A : Action<*, *>> actionOf(): A {
         val a = ActionManager.actionMap[A::class.java] ?: throw RuntimeException()
         return (a as ActionProvider<A, *, *>).create()
      }

      suspend inline fun <reified A : Action<*, *>> create(): A {
         val a = ActionManager.actionMap[A::class.java] ?: throw RuntimeException()
         return (a as ActionProvider<A, *, *>).create()
      }
   }

   inner class ActionJobCoroutine(
      parentContext: CoroutineContext
   ) : AbstractCoroutine<OUT>(parentContext, true), Subscription {
      @Suppress("UNCHECKED_CAST")
      override fun afterCompletion(state: Any?, mode: Int) {
         if (state is CompletedExceptionally) {
            if (state is Cancelled) {
               if (timedOut) {
                  breakerOperation.timeout()
                  if (!subscriber.isUnsubscribed)
                     subscriber.onError(ActionTimeoutException())
               } else {
                  if (!subscriber.isUnsubscribed)
                     subscriber.onError(ActionCancelledException())
               }
            } else {
               if (!subscriber.isUnsubscribed)
                  subscriber.onError(state.exception)
            }
         } else {
            if (!subscriber.isUnsubscribed)
               subscriber.onSuccess(state as OUT)
         }
      }

      // Subscription impl
      override fun isUnsubscribed(): Boolean = isCompleted

      override fun unsubscribe() {
         cancel()
      }
   }

   /**
    *
    */
   inner class ActionContextDispatcher(val actionContext: ActionContext,
                                       val eventLoop: ActionEventLoopContext) : CoroutineDispatcher() {
      override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
         return super.interceptContinuation(continuation)
      }

      override fun dispatch(context: CoroutineContext, block: Runnable) {
         if (Vertx.currentContext() != eventLoop) {
            eventLoop.runOnContext {
               breakerOperation.cpuStart()

               actionContext.currentTimeout = timesOutAt
               // Scope Action Context
               contextLocal.set(actionContext)
               try {
                  block.run()
               } finally {
                  contextLocal.remove()
                  breakerOperation.cpuEnd()
               }
            }
         } else {
            block.run()
         }
      }
   }
}
