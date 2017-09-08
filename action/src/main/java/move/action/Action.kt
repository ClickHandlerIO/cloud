package move.action

import com.google.common.base.Throwables
import io.vertx.circuitbreaker.CircuitBreakerState
import io.vertx.core.Vertx
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.ActorScope
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.rx2.asSingle
import kotlinx.coroutines.experimental.selects.SelectBuilder
import kotlinx.coroutines.experimental.selects.SelectInstance
import kotlinx.coroutines.experimental.selects.select
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext


object EmptyActionContext : IActionContext {
   override val deadline: Long
      get() = 0

}

interface IActionContext {
   //   val head: Action<*, *>
//   val startedAt: Long
   val deadline: Long
//   val startedBy: Action<*, *>
//   val current: Action<*, *>
//   val currentStartedAt: Long
//   val currentDeadline: Long
//
//   val tokenValue: String
//
//   /**
//    *
//    */
//   fun isHead(action: Action<*, *>) = head == action
}

class EventLoopDispatcher(val eventLoop: MoveEventLoop) : CoroutineDispatcher() {
   override fun dispatch(context: CoroutineContext, block: Runnable) {
      if (Vertx.currentContext() === eventLoop) {
         block.run()
      } else {
         eventLoop.runOnContext({ block.run() })
      }
   }
}

/*******************************************************************
 * Lifted FROM AbstractCoroutine!!!
 *******************************************************************/
@PublishedApi internal const val MODE_ATOMIC_DEFAULT = 0 // schedule non-cancellable dispatch for suspendCoroutine
@PublishedApi internal const val MODE_CANCELLABLE = 1    // schedule cancellable dispatch for suspendCancellableCoroutine
@PublishedApi internal const val MODE_DIRECT = 2         // when the context is right just invoke the delegate continuation direct
@PublishedApi internal const val MODE_UNDISPATCHED = 3   // when the thread is right, but need to mark it with current coroutine


abstract class AbstractInternalAction<A : Action<IN, OUT>, IN : Any, OUT : Any, P : ActionProvider<A, IN, OUT>> : JobAction<IN, OUT>(true), Continuation<OUT>, CoroutineContext, CoroutineScope {
   // Local fields.
   private var eventLoopActionId = 0L
   private lateinit var op: ActionCircuitBreakerMetrics.Operation
   private lateinit var _request: IN
   private var _deadline: Long = 0L
   // Provider
   lateinit var provider: P
      get
      private set

   lateinit var dispatcher: EventLoopDispatcher
   lateinit var block: suspend CoroutineScope.() -> OUT

   // Field helpers.
   val deadline: Long
      get() = _deadline

   val request: IN
      get() = _request

   val breaker
      get() = provider.breaker

   // Fallback
   open val maxRetries
      get() = 0

   /**
    * Flag to enable/disable fallback processing.
    * Use "shouldFallback()" method for more granular use cases.
    */
   open val isFallbackEnabled: Boolean
      get() = false

   fun isTimeOut(time: Long) = _deadline > 0 && time >= deadline

   @Suppress("LeakingThis")
   override val context: CoroutineContext = this
   override val coroutineContext: CoroutineContext get() = this
   override val hasCancellingState: Boolean get() = true

   internal open fun init(dispatcher: EventLoopDispatcher,
                          provider: P,
                          request: IN,
                          deadline: Long = 0,
                          start: CoroutineStart = CoroutineStart.DEFAULT,
                          parent: Boolean = false) {
      this.dispatcher = dispatcher
      this.provider = provider
      this._request = request
      this._deadline = deadline
      this.op = breaker.metrics.enqueue()

      initParentJob(dispatcher[Job])

      block = {
         val breaker = provider.breaker
         var rep: OUT? = null
         var state = breaker.state()

         // Begin operation
         op.begin()

         eventLoopActionId =
            if (deadline > 0L)
               dispatcher.eventLoop.registerAction(this@AbstractInternalAction, deadline)
            else
               dispatcher.eventLoop.registerAction(this@AbstractInternalAction)

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

            // Execute.
            rep = execute()
            breaker.reset()
            interceptReply(rep)
         } catch (e: Throwable) {
            var reason = e
            var cause = Throwables.getRootCause(e)

            if (e is CircuitOpenException) {
               op.shortCircuited()

               try {
                  if (isFallbackEnabled && shouldFallback(e, cause!!)) {
                     rep = executeFallback(reason, cause, true, 1)
                     op.fallbackSucceed()
                     rep
                  } else {
                     op.fallbackFailed()
                     throw e
                  }
               } catch (e2: Throwable) {
                  throw e2
               }
            } else if (e is ActionTimeoutException || cause is ActionTimeoutException) {
               throw e
            } else {
               breaker.incrementFailures()
               op.failed()

               // Ensure fallback is enabled.
               if (isFallbackEnabled && shouldFallback(e, cause!!)) {
                  var retryCount = 1

                  loop@
                  for (i in 0..maxRetries) {
                     if (retryCount <= maxRetries) {
                        try {
                           rep = executeFallback(reason, cause, false, retryCount)
                           op.fallbackSucceed()

                           break@loop
                        } catch (e2: Throwable) {
                           breaker.incrementFailures()
                           op.fallbackFailed()
                           reason = e2
                           cause = Throwables.getRootCause(e2)
                        }
                     } else {
                        op.failed()
                        throw e
                     }

                     retryCount++
                  }

                  // Ensure Reply is not null
                  if (rep == null)
                     throw NullPointerException("Reply cannot be null")
                  else
                     interceptReply(rep)
               } else {
                  throw e
               }
            }
         } finally {
            if (deadline > 0L)
               dispatcher.eventLoop.removeTimeOutAction(eventLoopActionId)
            else
               dispatcher.eventLoop.removeAction(eventLoopActionId)
         }
      }

      start(start)
   }

   internal open fun start(start: CoroutineStart) {
      start(block, this, this)
   }


   /**
    * @param request
    */
   abstract suspend fun execute(): OUT

   /**
    *
    */
   suspend open fun interceptReply(reply: OUT): OUT {
      return reply
   }

   internal fun systemException(caught: Throwable): Boolean {
      return when (caught) {
         is ActionTimeoutException, is CircuitOpenException -> true
         else -> false
      }
   }

   /**
    *
    */
   protected suspend open fun shouldFallback(caught: Throwable, cause: Throwable): Boolean {
      return when (cause) {
         is ActionTimeoutException -> false
         is TimeoutException -> false
         else -> false
      }
   }

   /**
    * @param request
    */
   protected suspend open fun executeFallback(caught: Throwable?,
                                              cause: Throwable?,
                                              circuitOpen: Boolean,
                                              retryCount: Int): OUT {
      // Default to running execute() again.
      return execute()
   }

   /**
    *
    */
   protected suspend open fun recover(caught: Throwable, cause: Throwable, isFallback: Boolean): OUT {
      throw cause
   }


   /**
    * Runs a coroutineBlock on the default vertx WorkerExecutor in a coroutine.
    */
   protected suspend fun <T> blocking(block: suspend () -> T): T = runBlocking { block() }
   // blocking(false, block)

//   /**
//    * Runs a coroutineBlock on the default vertx WorkerExecutor in a coroutine.
//    */
//   protected suspend fun <T> blocking(ordered: Boolean, block: suspend () -> T): T =
//      provider.vertx.rxExecuteBlocking<T>({
//         op.blockingBegin()
////         val old = Action._local.get()
////         Action._local.set(this)
//         try {
//            it.complete(runBlocking { block() })
//         } catch (e: Throwable) {
//            it.fail(e)
//         } finally {
////            Action._local.set(old)
//            op.blockingEnd()
//         }
//      }, ordered).await()
//
//   /**
//    * Runs a coroutineBlock on a specified vertx WorkerExecutor in a coroutine.
//    */
//   protected suspend fun <T> blocking(executor: WorkerExecutor, block: suspend () -> T): T =
//      executor.rxExecuteBlocking<T> {
//         op.blockingBegin()
////         val old = Action._local.get()
////         Action._local.set(this)
//         try {
//            it.complete(runBlocking { block() })
//         } catch (e: Throwable) {
//            it.fail(e)
//         } finally {
////            Action._local.set(old)
//            op.blockingEnd()
//         }
//      }.await()
//
//   /**
//    * Runs a coroutineBlock on a specified vertx WorkerExecutor in a coroutine.
//    */
//   suspend fun <T> blocking(executor: WorkerExecutor, ordered: Boolean, block: suspend () -> T): T =
//      executor.rxExecuteBlocking<T>({
//         op.blockingBegin()
////         val old = Action._local.get()
////         Action._local.set(this)
//         try {
//            it.complete(runBlocking { block() })
//         } catch (e: Throwable) {
//            it.fail(e)
//         } finally {
////            Action._local.set(old)
//            op.blockingEnd()
//         }
//      }, ordered).await()

   /**
    * Runs a coroutineBlock on the default vertx WorkerExecutor in a coroutine.
    */
   //   protected suspend fun <T> rxBlocking(block: suspend () -> T): rx.Single<T> = rxBlocking(false, block)

//   /**
//    * Runs a coroutineBlock on the default vertx WorkerExecutor in a coroutine.
//    */
//   suspend fun <T> rxBlocking(ordered: Boolean, block: suspend () -> T): rx.Single<T> {
//      return provider.vertx.rxExecuteBlocking<T>({
//         op.blockingBegin()
////         val old = Action._local.get()
////         Action._local.set(this)
//         try {
//            it.complete(runBlocking { block() })
//         } catch (e: Throwable) {
//            it.fail(e)
//         } finally {
////            Action._local.set(old)
//            op.blockingEnd()
//         }
//      }, ordered)
//   }

   /**
    * Runs a coroutineBlock on a specified vertx WorkerExecutor in a coroutine.
    */
   //   protected suspend fun <T> rxBlocking(executor: WorkerExecutor, block: suspend () -> T): rx.Single<T> =
//      rxBlocking(executor, false, block)
//
//   /**
//    * Runs a coroutineBlock on a specified vertx WorkerExecutor in a coroutine.
//    */
//   protected suspend fun <T> rxBlocking(executor: WorkerExecutor, ordered: Boolean, block: suspend () -> T): rx.Single<T> =
//      executor.rxExecuteBlocking<T>({
//         op.blockingBegin()
////         val old = Action._local.get()
////         Action._local.set(this)
//         try {
//            it.complete(runBlocking { block() })
//         } catch (e: Throwable) {
//            it.fail(e)
//         } finally {
////            Action._local.set(old)
//            op.blockingEnd()
//         }
//      }, ordered)


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
      suspendCancellableCoroutine<Unit> {
         dispatcher.eventLoop.registerTimer(it, unit.toMillis(time))
      }
   }

   /**
    * Synonym of delay
    */
   suspend fun sleep(time: Long, unit: TimeUnit = TimeUnit.MILLISECONDS) {
      return delay(time, unit)
   }


   override fun asSingle(): io.reactivex.Single<OUT> {
      return asSingle(dispatcher)
   }


   /*******************************************************************
    * Lifted FROM Deferred!!!
    *******************************************************************/


   override fun getCompleted(): OUT = getCompletedInternal() as OUT

   suspend override fun await(): OUT = awaitInternal() as OUT
   override fun <R> registerSelectAwait(select: SelectInstance<R>, block: suspend (OUT) -> R) =
      registerSelectAwaitInternal(select, block as (suspend (Any?) -> R))


   /*******************************************************************
    * Lifted FROM AbstractCoroutine!!!
    *******************************************************************/

   override fun resume(value: OUT) {
      loopOnState { state ->
         when (state) {
            is Incomplete -> if (updateState(state, value, MODE_ATOMIC_DEFAULT)) return
            is Cancelled -> return // ignore resumes on cancelled continuation
            else -> error("Already resumed, but got value $value")
         }
      }
   }

   override fun resumeWithException(exception: Throwable) {
      loopOnState { state ->
         when (state) {
            is Incomplete -> {
               if (updateState(state, CompletedExceptionally(exception), MODE_ATOMIC_DEFAULT)) return
            }
            is Cancelled -> {
               // ignore resumes on cancelled continuation, but handle exception if a different one is here
               if (exception !== state.exception) handleCoroutineException(context, exception)
               return
            }
            else -> throw IllegalStateException("Already resumed, but got exception $exception", exception)
         }
      }
   }

   override fun handleException(exception: Throwable) {
      handleCoroutineException(context, exception)
//         handleCoroutineException(parentContext, exception)
   }

   override fun afterCompletion(state: Any?, mode: Int) {
      super.afterCompletion(state, mode)

      // Operation finished.
      op.complete()

//         if (!dispatcher.isRoot) {
//            val parentOp = dispatcher.parent.action.op
//            parentOp.child += op.durationInNanos()
//            parentOp.childBlocking += op.blocking
//            parentOp.childTimeout = op.timeout
//         }

      if (state is Cancelled) {
         op.timeout()
      }
   }
}


/**
 *
 */
abstract class InternalAction<IN : Any, OUT : Any> : AbstractInternalAction<InternalAction<IN, OUT>, IN, OUT, InternalActionProvider<InternalAction<IN, OUT>, IN, OUT>>()

/**
 *
 */
abstract class InternalJob<IN : Any> : InternalAction<IN, Unit>()


/**
 * A WorkerAction is really just an InternalAction under the covers.
 */
abstract class WorkerAction<IN : Any, OUT : Any> : AbstractInternalAction<WorkerAction<IN, OUT>, IN, OUT, WorkerActionProvider<WorkerAction<IN, OUT>, IN, OUT>>() {
}

/**
 *
 */
abstract class WorkerJob<IN : Any> : WorkerAction<IN, Unit>()


/**
 *
 */
abstract class HttpAction : AbstractInternalAction<HttpAction, RoutingContext, Unit, HttpActionProvider<HttpAction>>() {
   val req
      get() = request.request()

   val resp
      get() = request.response()

   protected fun param(name: String) =
      req.getParam(name)
}


abstract class ActorAction<IN : Any, OUT : Any> : JobSupport(true), Continuation<OUT>, CoroutineScope, CoroutineContext, ActorScope<Any>, Channel<Any> {

}