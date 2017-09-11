package move.action

import com.google.common.base.Throwables
import io.reactivex.Single
import io.vertx.core.Vertx
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.ActorScope
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.rx2.asSingle
import kotlinx.coroutines.experimental.selects.SelectClause1
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.ContinuationInterceptor
import kotlin.coroutines.experimental.CoroutineContext


enum class CircuitBreakerState {
   CLOSED,
   HALF_OPEN,
   OPEN,
}

class ActionTimeoutException(val action: JobAction<*, *>) : CancellationException() {
   override fun toString(): String {
      return "ActionTimeout [${action.javaClass.canonicalName}]"
   }
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

/**
 * User-specified name of coroutine. This name is used in debugging mode.
 * See [newCoroutineContext] for the description of coroutine debugging facilities.
 */
data class ActionParent(
   /**
    * User-defined coroutine name.
    */
   val action: JobAction<*, *>
) : AbstractCoroutineContextElement(ActionParent) {
   /**
    * Key for [CoroutineName] instance in the coroutine context.
    */
   companion object Key : CoroutineContext.Key<ActionParent>

   /**
    * Returns a string representation of the object.
    */
   override fun toString(): String = "CoroutineName($action)"
}

object EmptyJobAction : JobAction<Unit, Unit>(false) {
   override fun asSingle(): Single<Unit> {
      return Single.just(Unit)
   }

   suspend override fun await() {

   }

   override fun getCompleted() {

   }

   override val onAwait: SelectClause1<Unit>
      get() = this as SelectClause1<Unit>
}

val EmptyActionParent = ActionParent(EmptyJobAction)


class MoveDispatcher(val eventLoop: MoveEventLoop) : CoroutineDispatcher(), Delay {
   var currentJob: Job? = null
//   var currentAction: AbstractJobAction<*, *, *, *>? = null

   fun currentAction(): AbstractJobAction<*, *, *, *>? {
      val job = (currentJob ?: return null) as? AbstractJobAction<*, *, *, *> ?: return null

      return job
   }

   fun execute(task: () -> Unit) = eventLoop.execute(task)

   override fun dispatch(context: CoroutineContext, block: Runnable) {
      if (Vertx.currentContext() === eventLoop) {
         currentJob = context[Job]
         block.run()
      } else {
         execute {
            currentJob = context[Job]
            block.run()
         }
      }
   }

   override fun scheduleResumeAfterDelay(time: Long, unit: TimeUnit, continuation: CancellableContinuation<Unit>) {
      eventLoop.scheduleDelay(continuation, unit.toMillis(time))
   }

   override fun invokeOnTimeout(time: Long, unit: TimeUnit, block: Runnable): DisposableHandle {
      if (Vertx.currentContext() === eventLoop) {
         // Run directly.
         block.run()
         // Already disposed.
         return EmptyDisposableHandle
      } else {
         val handle = MoveEventLoopDisposableHandle(block)
         // Needs to run on EventLoop.
         eventLoop.execute { handle.block?.run() }
         // Return disposable handle.
         return handle
      }
   }

   override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
      return super.interceptContinuation(continuation)
   }


}

object EmptyDisposableHandle : DisposableHandle {
   override fun dispose() {
   }
}

class MoveEventLoopDisposableHandle(@Volatile var block: Runnable?) : DisposableHandle {
   override fun dispose() {
      block = null
   }
}

data class Reply<T>(val value: T?, val cause: Throwable?)

/*******************************************************************
 * Lifted FROM AbstractCoroutine!!!
 *******************************************************************/
@PublishedApi internal const val MODE_ATOMIC_DEFAULT = 0 // schedule non-cancellable dispatch for suspendCoroutine
@PublishedApi internal const val MODE_CANCELLABLE = 1    // schedule cancellable dispatch for suspendCancellableCoroutine
@PublishedApi internal const val MODE_DIRECT = 2         // when the context is right just invoke the delegate continuation direct
@PublishedApi internal const val MODE_UNDISPATCHED = 3   // when the thread is right, but need to mark it with current coroutine

abstract class AbstractJobAction<A : Action<IN, OUT>, IN : Any, OUT : Any, P : ActionProvider<A, IN, OUT>> :
   JobAction<IN, OUT>(true),
   Continuation<OUT>,
   CoroutineContext,
   CoroutineScope,
   Delay,
   ContinuationInterceptor {

   override val key: CoroutineContext.Key<ContinuationInterceptor>
      get() = ContinuationInterceptor.Key

   private lateinit var _request: IN
   private var _deadline: Long = 0L
   // Provider
   lateinit var provider: P
      get
      private set
   lateinit var eventLoop: MoveEventLoop
   private var wheelDeadline = false

   // Field helpers.
   val deadline: Long
      get() = _deadline
   val request: IN
      get() = _request
   // Fallback
   open val maxRetries
      get() = 0

   /**
    * Flag to enable/disable fallback processing.
    * Use "shouldFallback()" method for more granular use cases.
    */
   open val isFallbackEnabled: Boolean
      get() = false

   var metrics: ActionMetrics? = null
   var parent: AbstractJobAction<*, *, *, *>? = null

//   @Suppress("LeakingThis")
//   var _context: CoroutineContext = this
//      get
//      private set

   override val context: CoroutineContext
      get() = this
   override val coroutineContext: CoroutineContext get() = context
   override val hasCancellingState: Boolean get() = true

   var starting = false

   /**
    * Launches action as the root coroutine.
    */
   internal open fun launch(eventLoop: MoveEventLoop,
                            provider: P,
                            request: IN,
                            timeoutTicks: Long = 0,
                            root: Boolean = false) {
//      if (MoveEventLoopGroup.currentEventLoop !== eventLoop) {
//         throw RuntimeException("Invoked from outside EventLoop thread.")
//      }

      this.metrics = ActionMetrics()

      this.eventLoop = eventLoop
      this.provider = provider
      this._request = request
      this._deadline = if (timeoutTicks < 1) 0 else eventLoop.epochTick + timeoutTicks

      if (root) {
         starting = true
         CoroutineStart.DEFAULT({ internalExecute() }, this, this)
      } else {
         val parent = eventLoop.currentJob

         if (parent == null) {
            starting = true
            CoroutineStart.DEFAULT({ internalExecute() }, this, this)
         } else {
//            _context = parent._context

            when {
               _deadline == 0L -> _deadline = parent._deadline
               _deadline >= parent._deadline ->
                  // Adjust timeoutTicks
                  // The parent will cancel this action so just for safety
                  // let's extend it with some padding to ensure it goes in
                  // a later Tick.
                  this._deadline = parent._deadline + 1
            }

//            _parent = WeakReference(parent)
            this.parent = parent
            starting = true
            CoroutineStart.DEFAULT({ internalExecute() }, this, this)
         }
      }
   }

   /**
    * Executes action "Inline".
    */
   internal open suspend fun execute1(eventLoop: MoveEventLoop,
                                      provider: P,
                                      request: IN,
                                      timeoutTicks: Long = 0,
                                      root: Boolean = false): OUT {
      launch(eventLoop, provider, request, timeoutTicks, root)
      return await()
   }

   /**
    * Executes action "Inline".
    */
   internal open suspend fun execute0(eventLoop: MoveEventLoop,
                                      provider: P,
                                      request: IN,
                                      timeoutTicks: Long = 0,
                                      root: Boolean = false): OUT {
      // Thread check.
//      if (MoveEventLoopGroup.currentEventLoop !== eventLoop) {
//         throw RuntimeException("Invoked from outside EventLoop thread.")
//      }

      this.metrics = ActionMetrics()

      this.eventLoop = eventLoop
      this.provider = provider
      this._request = request
      this._deadline = if (timeoutTicks < 1) 0 else eventLoop.epochTick + timeoutTicks

      if (root) {
         // Ignore the current job in the event loop.
         return wrapInternalExecute()
      } else {
         val parent = eventLoop.currentJob

         if (parent == null) {
            return wrapInternalExecute()
         } else {
            when {
               _deadline == 0L -> _deadline = parent._deadline
               _deadline >= parent._deadline ->
                  // Adjust timeoutTicks
                  // The parent will cancel this action so just for safety
                  // let's extend it with some padding to ensure it goes in
                  // a later Tick.
                  this._deadline = parent._deadline + 1
            }

            this.parent = parent
//            _parent = WeakReference(parent)

            return wrapInternalExecute()
         }
      }
   }

   internal fun doTimeout() {
      cancel(ActionTimeoutException(this))
   }

   /**
    *
    */
   inner class ActionContinuation<T>(val action: AbstractJobAction<*, *, *, *>?,
                                     val continuation: Continuation<T>) : Continuation<T> {
      override val context: CoroutineContext
         get() = continuation.context

      override fun resume(value: T) {
         val job = action ?: this@AbstractJobAction

         if (MoveEventLoopGroup.currentEventLoop !== eventLoop) {
            eventLoop.execute {
               eventLoop.currentJob = job
               try {
                  continuation.resume(value)
               } finally {
                  eventLoop.currentJob = job.parent
               }
            }
         } else {
            eventLoop.currentJob = job
            try {
               continuation.resume(value)
            } finally {
               eventLoop.currentJob = job.parent
            }
         }
      }

      override fun resumeWithException(exception: Throwable) {
         if (Vertx.currentContext() !== eventLoop) {
            eventLoop.execute {
               continuation.resumeWithException(exception)
            }
         } else {
            continuation.resumeWithException(exception)
         }
      }
   }

   /**
    *
    */
   override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
      if (starting) {
         starting = false
         return continuation
      }
      return ActionContinuation(eventLoop.currentJob, continuation)
   }

   /**
    *
    */
   internal suspend fun wrapInternalExecute(): OUT {
      try {
         val res = internalExecute()
//         afterInternalCompletion(null)
//         parent = null
         tryUpdateState(state!!, res)
         afterCompletion(state!!, 0)
//         updateState(state!!, res, MODE_ATOMIC_DEFAULT)
         return res
      } catch (e: Throwable) {
//         parent = null
         updateState(state!!, CompletedExceptionally(e), MODE_ATOMIC_DEFAULT)
//         afterInternalCompletion(e)
         throw e
      }
   }

   /**
    *
    */
   internal suspend fun internalExecute(): OUT {
      var rep: OUT? = null
      var state = provider.state()

      val previousJob = eventLoop.currentJob
      // Set current job.
      eventLoop.currentJob = this

      try {
         metrics?.begin = System.currentTimeMillis()
         metrics?.beginTick = eventLoop.epochTick

         // Do we have a deadline?
         if (_deadline > 0L)
            handle = eventLoop.registerJobTimerForTick(this, _deadline)
         else
            handle = eventLoop.registerJob(this)

         if (handle == null) {
            provider.incrementFailures()
            provider.timeout.increment()
            throw TimeoutException()
         }
      } catch (e: Throwable) {
         handle?.dispose()
         handle = null
         eventLoop.currentJob = previousJob
         throw e
      }

      try {
         onStart()

         when (state) {
            CircuitBreakerState.OPEN -> throw CircuitOpenException()
            CircuitBreakerState.HALF_OPEN -> {
               if (provider.passed.incrementAndGet() != 1) {
                  throw CircuitOpenException()
               }

               rep = execute()
               provider.reset()
               rep = interceptReply(rep)
               eventLoop.currentJob = previousJob
               rep
            }
            else -> {
               // Execute.
               rep = execute()
               rep = interceptReply(rep)
               eventLoop.currentJob = previousJob
               rep
            }
         }
      } catch (e: Throwable) {
         try {
            var reason = e
            var cause = Throwables.getRootCause(e)
            provider.failures.increment()

            if (e is CircuitOpenException) {
               metrics?.shortCircuited = true

               try {
                  if (isFallbackEnabled && shouldFallback(e, cause!!)) {
                     rep = executeFallback(reason, cause, true, 1)
                     metrics?.fallbackSucceed = true
                     handle?.dispose()
                     handle = null
                     eventLoop.currentJob = previousJob
                     rep
                  } else {
                     metrics?.fallbackFailed = true
                     handle?.dispose()
                     handle = null
                     eventLoop.currentJob = previousJob
                     throw e
                  }
               } catch (e2: Throwable) {
                  provider.incrementFailures()
                  handle?.dispose()
                  handle = null
                  eventLoop.currentJob = previousJob
                  throw e2
               }
            } else if (e is ActionTimeoutException || cause is ActionTimeoutException) {
               handle?.dispose()
               handle = null
               eventLoop.currentJob = previousJob
               throw e
            } else {
               // Ensure fallback is enabled.
               if (isFallbackEnabled && shouldFallback(e, cause!!)) {
                  var retryCount = 1

                  loop@
                  for (i in 0..maxRetries) {
                     if (retryCount <= maxRetries) {
                        try {
                           rep = executeFallback(reason, cause, false, retryCount)
                           metrics?.fallbackSucceed = true
                           break@loop
                        } catch (e2: Throwable) {
                           provider.incrementFailures()
                           metrics?.fallbackFailed = true
                           reason = e2
                           cause = Throwables.getRootCause(e2)
                        }
                     } else {
                        handle?.dispose()
                        handle = null
                        eventLoop.currentJob = previousJob
                        throw e
                     }

                     retryCount++
                  }

                  // Ensure Reply is not null
                  if (rep == null) {
                     eventLoop.currentJob = previousJob
                     throw NullPointerException("Reply cannot be null")
                  } else {
                     return interceptReply(rep)
                  }
               } else {
                  handle?.dispose()
                  handle = null
                  eventLoop.currentJob = previousJob
                  throw e
               }
            }
         } catch (e: Throwable) {
            handle?.dispose()
            handle = null
            eventLoop.currentJob = previousJob
            throw e
         }
      }

      return rep!!
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

   /**
    *
    */
   internal fun systemException(caught: Throwable): Boolean {
      return when (caught) {
         is TimeoutException, is CircuitOpenException -> true
         else -> false
      }
   }

   /**
    *
    */
   protected suspend open fun shouldFallback(caught: Throwable, cause: Throwable): Boolean {
      return when (cause) {
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
    *
    */
   protected suspend fun <T> blocking(block: suspend () -> T): T =
      suspendCancellableCoroutine { cont ->
         provider.vertx.executeBlocking<T>(
            {
               metrics?.blockingBegin = System.currentTimeMillis()
               try {
                  val result = runBlocking { block.invoke() }
                  if (metrics != null)
                     metrics!!.blocking += (System.currentTimeMillis() - metrics!!.blockingBegin)
                  it.complete(result)
               } catch (e: Throwable) {
                  if (metrics != null)
                     metrics!!.blocking += (System.currentTimeMillis() - metrics!!.blockingBegin)
                  it.fail(e)
               }
            },
            {
               if (it.failed()) {
                  cont.resumeWithException(it.cause())
               } else {
                  cont.resume(it.result())
               }
            }
         )
      }

   /**
    *
    */
   protected suspend fun <T> rxBlocking(block: suspend () -> T): Deferred<T> =
      async(coroutineContext) {
         blocking(block)
      }

   /**
    *
    */
   protected suspend fun <T> blocking(executor: ExecutorService, block: suspend () -> T): T =
      suspendCancellableCoroutine { cont ->
         try {
            executor.execute {
               metrics?.blockingBegin = System.currentTimeMillis()
               try {
                  val result = runBlocking {
                     try {
                        block.invoke()
                     } catch (e: Throwable) {
                        throw e
                     }
                  }
                  if (metrics != null)
                     metrics!!.blocking += (System.currentTimeMillis() - metrics!!.blockingBegin)

                  result
               } catch (e: Throwable) {
                  if (metrics != null)
                     metrics!!.blocking += (System.currentTimeMillis() - metrics!!.blockingBegin)
                  cont.resumeWithException(e)
               }
            }
         } catch (e: Throwable) {
            cont.resumeWithException(e)
         }
      }

   protected suspend fun <T> rxBlocking(executor: ExecutorService, block: suspend () -> T): Deferred<T> =
      async(coroutineContext) {
         blocking(executor, block)
      }

   suspend fun <T> tryWhile(deferred: suspend () -> T, block: (Throwable) -> Boolean): T {
      var retries = 3

      while (retries > 0) {
         try {
            return deferred.invoke()
         } catch (e: Throwable) {
            if (e is ActionTimeoutException) {
               throw e
            }

            if (!block.invoke(e)) {
               throw e
            }
         }
         retries--
      }

      throw TryWhileException()
   }

   /**
    *
    */
   suspend override fun delay(time: Long, unit: TimeUnit) {
      require(time >= 0) { "Delay time $time cannot be negative" }
      if (time <= 0) return // don't delay
      return suspendCancellableCoroutine { scheduleResumeAfterDelay(time, unit, it) }
   }

   /**
    *
    */
   override fun invokeOnTimeout(time: Long, unit: TimeUnit, block: Runnable): DisposableHandle {
      if (MoveEventLoopGroup.currentEventLoop === eventLoop) {
         // Run directly.
         block.run()
         // Already disposed.
         return EmptyDisposableHandle
      } else {
         val handle = MoveEventLoopDisposableHandle(block)
         // Needs to run on EventLoop.
         eventLoop.execute { handle.block?.run() }
         // Return disposable handle.
         return handle
      }
   }

   /**
    * Schedule after delay.
    * EventLoop has a WheelTimer with 100ms precision.
    * Do not use "delay" if nanosecond precision is needed.
    */
   override fun scheduleResumeAfterDelay(time: Long, unit: TimeUnit, continuation: CancellableContinuation<Unit>) {
      eventLoop.scheduleDelay(continuation, unit.toMillis(time))
   }

   /**
    *
    */
   override fun asSingle(): io.reactivex.Single<OUT> {
      return asSingle(this)
   }

   /*******************************************************************
    * Lifted FROM Deferred!!!
    *******************************************************************/


   override fun getCompleted(): OUT = getCompletedInternal() as OUT

   suspend override fun await(): OUT = awaitInternal() as OUT
   override val onAwait: SelectClause1<OUT>
      get() = this as SelectClause1<OUT>


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
   }

//   fun afterCompletionInternal(state: Any?, e: Throwable) {
//      if (handle != null) {
//         handle?.dispose()
//         handle = null
//      }
//
//      // !!! Important !!! Ensure we clean up reference to Parent
//      parent = null
//
//      val duration = System.currentTimeMillis() - begin
//      if (duration > 0)
//         provider.durationMs.add(duration)
//
//      if (state is CompletedExceptionally) {
//         if (state.exception is ActionTimeoutException || state.cause is ActionTimeoutException) {
//            provider.timeout.increment()
//         }
//      } else if (state is Cancelled) {
//         if (state.exception is ActionTimeoutException || state.cause is ActionTimeoutException) {
//            provider.timeout.increment()
//         }
//      } else {
//         provider.success.increment()
//      }
//   }

   override fun afterCompletion(state: Any?, mode: Int) {
      if (handle != null) {
         handle?.dispose()
         handle = null
      }

      // !!! Important !!! Ensure we clean up reference to Parent
      parent = null

      if (metrics != null) {
         // Capture end tick.
         metrics?.endTick = eventLoop.epochTick

         val duration = System.currentTimeMillis() - metrics!!.begin
         if (duration > 0)
            provider.durationMs.add(duration)
      }

      if (state is CompletedExceptionally) {
         if (state.exception is ActionTimeoutException || state.cause is ActionTimeoutException) {
            provider.timeout.increment()
         }
      } else if (state is Cancelled) {
         if (state.exception is ActionTimeoutException || state.cause is ActionTimeoutException) {
            provider.timeout.increment()
         }
      } else {
         provider.success.increment()
      }
   }

   class ActionMetrics {
      // Metrics.
      var begin: Long = 0
      var beginTick: Long = 0
      var endTick: Long = 0
      var cpu: Long = 0
      var cpuBegin: Long = 0
      var blocking: Long = 0
      var blockingBegin: Long = 0
      var child: Long = 0
      var childCpu: Long = 0
      var childBlocking: Long = 0

      var shortCircuited = false
      var fallbackSucceed = false
      var fallbackFailed = false
      var failed = false
   }
}


/**
 *
 */
abstract class InternalAction<IN : Any, OUT : Any> : AbstractJobAction<InternalAction<IN, OUT>, IN, OUT, InternalActionProvider<InternalAction<IN, OUT>, IN, OUT>>()

/**
 *
 */
abstract class InternalJob<IN : Any> : InternalAction<IN, Unit>()


/**
 * A WorkerAction is really just an InternalAction under the covers.
 */
abstract class WorkerAction<IN : Any, OUT : Any> : AbstractJobAction<WorkerAction<IN, OUT>, IN, OUT, WorkerActionProvider<WorkerAction<IN, OUT>, IN, OUT>>() {
}

/**
 *
 */
abstract class WorkerJob<IN : Any> : WorkerAction<IN, Unit>()


/**
 *
 */
abstract class HttpAction : AbstractJobAction<HttpAction, RoutingContext, Unit, HttpActionProvider<HttpAction>>() {
   val req
      get() = request.request()

   val resp
      get() = request.response()

   protected fun param(name: String) =
      req.getParam(name)
}


abstract class ActorAction<IN : Any, OUT : Any> : JobSupport(true), Continuation<OUT>, CoroutineScope, CoroutineContext, ActorScope<Any>, Channel<Any> {

}


class TryWhileException : RuntimeException()