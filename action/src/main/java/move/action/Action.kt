package move.action

import com.google.common.base.Throwables
import io.vertx.circuitbreaker.CircuitBreakerState
import io.vertx.core.impl.ActionEventLoopContext
import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.core.WorkerExecutor
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.rx1.await
import kotlinx.coroutines.experimental.rx1.rxSingle
import rx.Single
import rx.SingleSubscriber
import rx.Subscription
import java.util.concurrent.TimeoutException
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.startCoroutine
import kotlin.coroutines.experimental.suspendCoroutine
import kotlin.reflect.KClass

val Actions = ActionManager.Companion

/**
 * @author Clay Molocznik
 */
abstract class Action<IN : Any, OUT : Any> : IAction<IN, OUT>() {
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
   private lateinit var coroutine: ActionJobCoroutine
      get

   // Action Context
   val context: ActionContext
      get() = dispatcher.actionContext

   // Rx
   private lateinit var single: Single<OUT>
   private lateinit var subscriber: SingleSubscriber<in OUT>

   // Local fields
   private var timedOut: Boolean = false
   private var timesOutAt: Long = 0L
   private var reason: Throwable? = null
   private var cause: Throwable? = null

   // Fallback
   private var retryCount = 0
   private var maxRetries = 1
   private var reply: OUT? = null

   // ID
   private var actionId = 0L

   // Remote
   private var remoteActionId: String? = null
   private var remoteVisibleUntil: Long = 0

   /**
    * Flag to enable/disable fallback processing.
    * Use "shouldFallback()" method for more granular use cases.
    */
   open val isFallbackEnabled: Boolean
      get() = false

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

                  // Always return a non-null value
                  if (rep == null)
                     provider.outProvider.get()
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

   fun isTimeOut(time: Long) = time >= context.timesOutAt

   open fun afterInit() {
   }

   private fun start() {
      coroutineBlock.startCoroutine(coroutine, coroutine)
   }

   open suspend fun onStart() {
   }

   fun timedOut() {
      if (coroutine.isActive) {
         timedOut = true
         coroutine.cancel(CancellationException())
      }
   }

   suspend fun suspend(block: (Continuation<OUT>) -> Unit): OUT {
      return suspendCoroutine {
         block(it)
      }
   }

   /**
    * Runs a coroutineBlock on the default vertx WorkerExecutor in a coroutine.
    */
   suspend fun <T> blocking(block: suspend () -> T): T = blocking(false, block)

   /**
    * Runs a coroutineBlock on the default vertx WorkerExecutor in a coroutine.
    */
   suspend fun <T> blocking(ordered: Boolean, block: suspend () -> T): T =
      provider.vertx.rxExecuteBlocking<T>({
         try {
            it.complete(runBlocking { block() })
         } catch (e: Throwable) {
            it.fail(e)
         }
      }, ordered).await()

   /**
    * Runs a coroutineBlock on a specified vertx WorkerExecutor in a coroutine.
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
    * Runs a coroutineBlock on a specified vertx WorkerExecutor in a coroutine.
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
    * Runs a coroutineBlock on the default vertx WorkerExecutor in a coroutine.
    */
   suspend fun <T> worker(block: suspend () -> T): Single<T> = worker(false, block)

   /**
    * Runs a coroutineBlock on the default vertx WorkerExecutor in a coroutine.
    */
   suspend fun <T> blockingSingle(block: suspend () -> T): Single<T> = worker(false, block)

   /**
    * Runs a coroutineBlock on the default vertx WorkerExecutor in a coroutine.
    */
   suspend fun <T> worker(ordered: Boolean, block: suspend () -> T): Single<T> =
      provider.vertx.rxExecuteBlocking<T>({
         try {
            it.complete(runBlocking { block() })
         } catch (e: Throwable) {
            it.fail(e)
         }
      }, ordered)

   /**
    * Runs a coroutineBlock on a specified vertx WorkerExecutor in a coroutine.
    */
   suspend fun <T> worker(executor: WorkerExecutor, block: suspend () -> T): Single<T> = worker(executor, false, block)

   /**
    * Runs a coroutineBlock on a specified vertx WorkerExecutor in a coroutine.
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
   protected fun <T> single(block: suspend CoroutineScope.() -> T): Single<T> = rxSingle(dispatcher, block)

   /**
    *
    */
   inline fun request(block: IN.() -> Unit): IN = provider.inProvider.get().apply(block)

   /**
    *
    */
   inline fun reply(block: OUT.() -> Unit): OUT = provider.outProvider.get().apply(block)

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
    *
    */
   val rx: Single<OUT>
      get() {
         if (_request != null) {
            return provider.create().rx(request)
         }

         _request = provider.inProvider.get()
         return single
      }

   fun rx(request: IN): Single<OUT> {
      if (_request != null) {
         return provider.create().rx(request)
      }

      _request = request
      return single
   }

   fun rx(block: IN.() -> Unit): Single<OUT> {
      if (_request != null) {
         return provider.create().rx(block)
      }

      _request = provider.inProvider.get().apply(block)
      return single
   }

   suspend fun invoke(request: IN): OUT {
      if (_request != null) {
         return provider.create().invoke(request)
      }

      _request = request
      return single.await()
   }

//   suspend operator fun <A : Action<IN, OUT>, IN : Any, OUT : Any> invoke(cls: KClass<A>, request: IN): OUT {
//      val provider = Actions.actionMap[cls.java]
//
//      if (provider == null)
//         throw ActionCancelledException()
//
//      return (provider.create() as A).invoke(request)
//   }

   /**
    *
    */
   suspend operator fun invoke(block: IN.() -> Unit): OUT {
      if (_request != null) {
         return provider.create().invoke(block)
      }

      _request = provider.inProvider.get().apply(block)
      return single.await()
   }

   /**
    *
    */
   suspend fun <A : Action<IN, OUT>, IN : Any, OUT : Any> of(cls: KClass<A>): A {
      val provider = Actions.actionMap[cls.java]

      if (provider == null)
         throw ActionCancelledException()

      return (provider.create() as A)
   }

   /**
    *
    */
   suspend fun <A : Action<IN, OUT>, IN : Any, OUT : Any> invoke(cls: KClass<A>, block: suspend IN.() -> Unit): OUT {
      val provider = Actions.actionMap[cls.java]

      if (provider == null)
         throw ActionCancelledException()

      val request = provider.inProvider.get() as IN
      block.invoke(request)

      return (provider.create() as A).invoke(request)
   }

   /**
    *
    */
   suspend fun await(request: IN): OUT {
      if (_request == null) {
         return provider.create().await(request)
      }

      _request = request
      return single.await()
   }

   /**
    *
    */
   suspend fun invokeLater(request: IN): Unit {
      if (_request != null) {
         provider.create().invokeLater(request)
         return
      }

      _request = request
      single.subscribe()
   }

   /**
    *
    */
   suspend fun invokeLater(block: IN.() -> Unit): Unit {
      if (_request != null) {
         provider.create().invokeLater(block)
         return
      }
      _request = provider.inProvider.get().apply(block)
      single.subscribe()
   }

   /**
    *
    */
   suspend fun forceInvoke(request: IN): OUT {
      if (_request != null) {
         provider.create().forceInvoke(request)
      }

      _request = request
      return single.await()
   }

   companion object {
      val contextLocal = ThreadLocal<ActionContext?>()

      fun currentContext(): ActionContext? {
         return contextLocal.get()
      }

      inline fun <reified A : Action<IN, OUT>, IN : Any, OUT : Any> providerOf(): ActionProvider<A, IN, OUT>? {
         return ActionManager.actionMap[A::class.java] as ActionProvider<A, IN, OUT>
      }

      suspend fun <A : Action<IN, OUT>, IN : Any, OUT : Any> new(cls: KClass<A>): A {
         val provider = Actions.actionMap[cls.java]

         if (provider == null)
            throw ActionCancelledException()

         return (provider.create() as A)
      }

      inline fun <reified A : Action<*, *>> new(): A {
         val a = ActionManager.actionMap[A::class.java]
         if (a == null) {
            throw RuntimeException()
         }

         return (a as ActionProvider<A, *, *>).create()
      }

      inline fun <reified A : Action<*, *>> new(block: A.() -> Unit): A {
         val a = ActionManager.actionMap[A::class.java]
         if (a == null) {
            throw RuntimeException()
         }

         val action = (a as ActionProvider<A, *, *>).create()
         action.apply(block)
         return action
      }


      inline fun <reified A : Action<*, *>> of(): A {
         val a = ActionManager.actionMap[A::class.java]
         if (a == null) {
            throw RuntimeException()
         }

         return (a as ActionProvider<A, *, *>).create()
      }

      inline fun <reified A : Action<*, *>> instanceOf(): A {
         val a = ActionManager.actionMap[A::class.java]
         if (a == null) {
            throw RuntimeException()
         }

         return (a as ActionProvider<A, *, *>).create()
      }

      suspend inline fun <reified A : Action<*, *>> action(): A {
         val a = ActionManager.actionMap[A::class.java] ?: throw RuntimeException()
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

   suspend open fun afterExecute(reply: OUT): OUT {
      return reply
   }

   inner class ActionJobCoroutine(
      parentContext: CoroutineContext
   ) : AbstractCoroutine<OUT>(parentContext, true), Subscription {
      override fun onStart() {
      }

      override fun onCancellation() {
         super.onCancellation()
      }

      override fun onParentCancellation(cause: Throwable?) {
         super.onParentCancellation(cause)
      }

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
      override fun dispatch(context: CoroutineContext, block: Runnable) {
         if (Vertx.currentContext() != eventLoop) {
            eventLoop.runOnContext {
               actionContext.currentTimeout = timesOutAt
               // Scope Action Context
               contextLocal.set(actionContext)
               try {
                  block.run()
               } finally {
                  contextLocal.remove()
               }
            }
         } else {
            block.run()
         }
      }
   }
}
