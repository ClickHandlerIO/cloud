package move.action

import com.google.common.base.Throwables
import io.netty.buffer.ByteBuf
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.rx1.asSingle
import kotlinx.coroutines.experimental.selects.SelectClause1
import move.threading.WorkerPool
import rx.Single
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.function.Supplier
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.ContinuationInterceptor
import kotlin.coroutines.experimental.CoroutineContext


interface HasTimers {
   fun addTimer(handle: TimerHandle)

   fun removeTimer(handle: TimerHandle)

   fun onTimer(handle: TimerEventHandle)
}


enum class CircuitBreakerState {
   CLOSED,
   /**
    * State when the circuit is OPEN and a single
    * request is allowed through to try to get it
    * back to a CLOSED state.
    */
   HALF_OPEN,
   OPEN,
}

interface ActionToken {
   val secured: Boolean
}

object NoToken : ActionToken {
   override val secured: Boolean = false
}

interface DeferredAction<T> : Deferred<T> {
   fun asSingle(): Single<T>
}

class ActionTimeoutException(val action: IJobAction<*, *>) : CancellationException() {
   override fun toString(): String {
      return "ActionTimeout [${action.javaClass.canonicalName}]"
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

/*******************************************************************
 * Lifted FROM AbstractCoroutine!!!
 *******************************************************************/
@PublishedApi internal const val MODE_ATOMIC_DEFAULT = 0 // schedule non-cancellable dispatch for suspendCoroutine
@PublishedApi internal const val MODE_CANCELLABLE = 1    // schedule cancellable dispatch for suspendCancellableCoroutine
@PublishedApi internal const val MODE_DIRECT = 2         // when the context is right just invoke the delegate continuation direct
@PublishedApi internal const val MODE_UNDISPATCHED = 3   // when the thread is right, but need to mark it with current coroutine

/**
 *
 */
abstract class JobAction<IN : Any, OUT : Any> :
   IJobAction<IN, OUT>(true),
   Continuation<OUT>,
   CoroutineContext,
   CoroutineScope,
   Delay,
   ContinuationInterceptor,
   HasTimers {

   private var _timers: ArrayList<TimerHandle>? = null

   // The "root" action is the "Dispatcher".
   // The assigned MEventLoop is used.
   override val key: CoroutineContext.Key<ContinuationInterceptor>
      get() = ContinuationInterceptor.Key

   private lateinit var _request: IN
   private var _deadline: Long = 0L
   // Provider
   lateinit var provider: ActionProvider<*, IN, OUT>
      get
      private set
   lateinit var eventLoop: MEventLoop
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

   var parent: JobAction<*, *>? = null
      get
      private set

   var token: ActionToken? = null
      get
      private set

   var _actor: AbstractActorAction<*>? = null

   @Suppress("LeakingThis")
   var _context: CoroutineContext = this
      get
      private set

   override val context: CoroutineContext
      get() = _context
   override val coroutineContext: CoroutineContext get() = context
   override val hasCancellingState: Boolean get() = true

   var starting = false
   var currentContinuation: Continuation<*>? = null


   var metrics = false
   var tick: Boolean = true
   // Metrics.
   var begin: Long = 0
   var endTick: Long = 0
   var cpu: Long = 0
   var cpuBegin: Long = 0
   var blocking: Long = 0
   var child: Long = 0
   var childCpu: Long = 0
   var childBlocking: Long = 0

   var shortCircuited = false
   var fallbackSucceed = false
   var fallbackFailed = false
   var failed = false

   internal fun begin() {
//      if (!metrics)
//         return

      begin = if (tick)
         eventLoop.epochTick
      else
         System.currentTimeMillis()
   }

   internal fun end() {
//      if (!metrics)
//         return

      val elapsed = if (tick)
         eventLoop.epochTick - begin
      else
         System.currentTimeMillis() - begin

//      provider.durationMs.add(elapsed)
   }

   @Synchronized internal fun addBlockingTime(time: Long) {
      blocking += time
   }

   override fun addTimer(handle: TimerHandle) {
      val _timers = this._timers
      if (_timers == null) {
         this._timers = arrayListOf(handle)
      } else {
         _timers.add(handle)
      }
   }

   override fun removeTimer(handle: TimerHandle) {
      val _timers = this._timers
      if (_timers != null) {
         _timers -= handle
         if (_timers.isEmpty()) {
            this._timers = null
         }
      }
   }

   override fun onTimer(handle: TimerEventHandle) {
   }

   private fun removeAllTimers() {
      _timers?.apply { forEach { it.remove() }; clear() }
      _timers = null
   }

   /**
    * Launches action as the root coroutine.
    */
   internal open fun launch(eventLoop: MEventLoop,
                            provider: ActionProvider<*, IN, OUT>,
                            request: IN,
                            token: ActionToken,
                            timeoutTicks: Long = 0,
                            root: Boolean = false) {
//      if (MKernel.currentEventLoop !== eventLoop) {
//         throw RuntimeException("Invoked from outside EventLoop thread.")
//      }

      this.token = token
      this.eventLoop = eventLoop
      this.provider = provider
      this._request = request
      this._deadline = if (timeoutTicks < 1) 0 else eventLoop.epochTick + timeoutTicks

      if (root) {
         starting = true
         CoroutineStart.DEFAULT({ internalExecute() }, this, this)
      } else {
         val parent = eventLoop.job

         if (parent == null) {
            starting = true
            CoroutineStart.DEFAULT({ internalExecute() }, this, this)
         } else {
            _context = parent._context + this
            this.token = parent.token

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
   internal open suspend fun execute0(eventLoop: MEventLoop,
                                      provider: ActionProvider<*, IN, OUT>,
                                      request: IN,
                                      timeoutTicks: Long = 0,
                                      root: Boolean = false): OUT {
      launch(eventLoop, provider, request, NoToken, timeoutTicks, root)
      return await()
   }

   /**
    * Executes action "Inline".
    */
   internal open suspend fun execute1(eventLoop: MEventLoop,
                                      provider: ActionProvider<*, IN, OUT>,
                                      request: IN,
                                      timeoutTicks: Long = 0,
                                      root: Boolean = false): OUT {
      // Thread check.
//      if (MKernel.currentEventLoop !== eventLoop) {
//         throw RuntimeException("Invoked from outside EventLoop thread.")
//      }

//      this.metrics = ActionMetrics()

      this.eventLoop = eventLoop
      this.provider = provider
      this._request = request
      this._deadline = if (timeoutTicks < 1) 0 else eventLoop.epochTick + timeoutTicks

      if (root) {
         // Ignore the current job in the event loop.
         return wrapInternalExecute()
      } else {
         val parent = eventLoop.job

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

            this._context = parent._context
            this.parent = parent
            return wrapInternalExecute()
         }
      }
   }

   override fun doTimeout() {
      val exception = ActionTimeoutException(this)
//      try {
      cancel(exception)
//      } finally {
//         currentContinuation?.resumeWithException(exception)
//      }
   }

   suspend fun <T> suspendAction(supplier: java.util.function.Function<CancellableContinuation<T>, T>): T {
      return suspendCancellableCoroutine { cont ->
         try {
            cont.resume(supplier.apply(cont))
         } catch (e: Throwable) {
            cont.resumeWithException(e)
         }
      }
   }

   /**
    *
    */
   inner class ActionContinuation<T>(val action: JobAction<*, *>?,
                                     val continuation: Continuation<T>) : Continuation<T> {
      override val context: CoroutineContext
         get() = continuation.context

      override fun resume(value: T) {
         val job = action ?: eventLoop.job

         if (MKernel.currentEventLoop !== eventLoop) {
            eventLoop.execute {
               eventLoop.job = job

               if (job?.isCancelled == true) {
                  continuation.resumeWithException(ActionTimeoutException(job))
               } else {
                  continuation.resume(value)
               }
            }
         } else {
            eventLoop.job = job

            if (job?.isCancelled == true) {
               continuation.resumeWithException(ActionTimeoutException(job))
            } else {
               continuation.resume(value)
            }
         }
      }

      override fun resumeWithException(exception: Throwable) {
         val job = action ?: eventLoop.job

         if (Vertx.currentContext() !== eventLoop) {
            eventLoop.execute {
               eventLoop.job = job

               if (job?.isCancelled == true) {
                  continuation.resumeWithException(ActionTimeoutException(job))
               } else {
                  continuation.resumeWithException(exception)
               }
            }
         } else {
            eventLoop.job = job

            if (job?.isCancelled == true) {
               continuation.resumeWithException(ActionTimeoutException(job))
            } else {
               continuation.resumeWithException(exception)
            }
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

      val continuationContext = continuation.context
      val job = eventLoop.job

      val actionContinuation = ActionContinuation(job, continuation)
      job?.currentContinuation = actionContinuation

      return actionContinuation
   }

   /**
    *
    */
   internal suspend fun wrapInternalExecute(): OUT {
      try {
         val res = internalExecute()
         updateState(state!!, res, MODE_ATOMIC_DEFAULT)
         return res
      } catch (e: Throwable) {
         updateState(state!!, CompletedExceptionally(e), MODE_ATOMIC_DEFAULT)
         throw e
      }
   }

   /**
    *
    */
   internal suspend fun internalExecute(): OUT {
      var rep: OUT? = null
      var state = provider.state()

      // Set current job.
      eventLoop.job = this

      try {
         begin()

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
         throw e
      }

      try {
         when (state) {
            CircuitBreakerState.OPEN -> throw CircuitOpenException()
            CircuitBreakerState.HALF_OPEN -> {
               if (provider.passed.incrementAndGet() != 1) {
                  throw CircuitOpenException()
               }

               rep = execute()
               provider.reset()
               eventLoop.job = this
               rep = interceptReply(rep)
               eventLoop.job = this
            }
            else -> {
               // Execute.
               rep = execute()
               eventLoop.job = this
               rep = interceptReply(rep)
               eventLoop.job = this
            }
         }
      } catch (e: Throwable) {
         try {
            var reason = e
            var cause = Throwables.getRootCause(e)
            provider.failures.increment()

            if (e is CircuitOpenException) {
               shortCircuited = true

               try {
                  if (isFallbackEnabled && shouldFallback(e, cause!!)) {
                     rep = executeFallback(reason, cause, true, 1)
                     fallbackSucceed = true
                     handle?.dispose()
                     handle = null
                     eventLoop.job = this
                  } else {
                     fallbackFailed = true
                     handle?.dispose()
                     handle = null
                     eventLoop.job = this
                     throw e
                  }
               } catch (e2: Throwable) {
                  provider.incrementFailures()
                  handle?.dispose()
                  handle = null
                  eventLoop.job = this
                  throw e2
               }
            } else if (e is ActionTimeoutException || cause is ActionTimeoutException) {
               handle?.dispose()
               handle = null
               eventLoop.job = this
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
                           fallbackSucceed = true
                           break@loop
                        } catch (e2: Throwable) {
                           provider.incrementFailures()
                           fallbackFailed = true
                           reason = e2
                           cause = Throwables.getRootCause(e2)
                        }
                     } else {
                        handle?.dispose()
                        handle = null
                        eventLoop.job = this
                        throw e
                     }

                     retryCount++
                  }

                  // Ensure Reply is not null
                  if (rep == null) {
                     eventLoop.job = this
                     throw NullPointerException("Reply cannot be null")
                  } else {
                     return interceptReply(rep)
                  }
               } else {
                  handle?.dispose()
                  handle = null
                  eventLoop.job = this
                  throw e
               }
            }
         } catch (e: Throwable) {
            handle?.dispose()
            handle = null
            eventLoop.job = this
            throw e
         }
      }

      return rep!!
   }


   /**
    * @param request
    */
   abstract suspend protected fun execute(): OUT

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
   protected open fun shouldFallback(caught: Throwable, cause: Throwable): Boolean {
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
         eventLoop.executeBlocking<T>(Handler {
            val begin = System.currentTimeMillis()
            try {
               val result = runBlocking { block() }
               addBlockingTime(begin - System.currentTimeMillis())
               try {
                  it.complete(result)
               } catch (e: Throwable) {
                  // Ignore.
               }
            } catch (e: Throwable) {
               addBlockingTime(begin - System.currentTimeMillis())
               eventLoop.execute { it.fail(e) }
            }
         }, Handler {
            if (it.failed()) {
               eventLoop.execute { cont.resumeWithException(it.cause()) }
            } else {
               eventLoop.execute { cont.resume(it.result()) }
            }
         })
      }

   /**
    *
    */
   protected suspend fun <T> javaBlocking(block: Supplier<T>): T =
      suspendCancellableCoroutine { cont ->
         eventLoop.executeBlocking<T>(Handler {
            val begin = System.currentTimeMillis()
            try {
               val result = runBlocking { block.get() }

               addBlockingTime(begin - System.currentTimeMillis())
               try {
                  it.complete(result)
               } catch (e: Throwable) {
                  // Ignore.
               }
            } catch (e: Throwable) {
               addBlockingTime(begin - System.currentTimeMillis())
               eventLoop.execute { it.fail(e) }
            }
         }, Handler {
            if (it.failed()) {
               eventLoop.execute { cont.resumeWithException(it.cause()) }
            } else {
               eventLoop.execute { cont.resume(it.result()) }
            }
         })
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
               val begin = System.currentTimeMillis()

               try {
                  val result = runBlocking {
                     try {
                        block.invoke()
                     } catch (e: Throwable) {
                        throw e
                     }
                  }

                  addBlockingTime(begin - System.currentTimeMillis())

                  try {
                     eventLoop.execute { cont.resume(result) }
                  } catch (e: Throwable) {
                     // Ignore.
                  }
               } catch (e: Throwable) {
                  addBlockingTime(begin - System.currentTimeMillis())
                  eventLoop.execute {
                     cont.resumeWithException(e)
                  }
               }
            }
         } catch (e: Throwable) {
            cont.resumeWithException(e)
         }
      }

   /**
    *
    */
   protected suspend fun <T> javaBlocking(executor: ExecutorService, block: Supplier<T>): T =
      suspendCancellableCoroutine { cont ->
         try {
            executor.execute {
               val begin = System.currentTimeMillis()
               try {
                  val result = runBlocking {
                     try {
                        block.get()
                     } catch (e: Throwable) {
                        throw e
                     }
                  }
                  addBlockingTime(begin - System.currentTimeMillis())
                  try {
                     eventLoop.execute { cont.resume(result) }
                  } catch (e: Throwable) {
                     // Ignore.
                  }
               } catch (e: Throwable) {
                  addBlockingTime(begin - System.currentTimeMillis())
                  eventLoop.execute {
                     cont.resumeWithException(e)
                  }
               }
            }
         } catch (e: Throwable) {
            cont.resumeWithException(e)
         }
      }

   protected suspend fun <T> rxBlocking(executor: ExecutorService, block: suspend () -> T): Deferred<T> =
      async(context) {
         blocking(executor, block)
      }

   /**
    *
    */
   protected suspend fun <T> blocking(pool: WorkerPool, block: suspend () -> T): T =
      suspendCancellableCoroutine { cont ->
         pool.executeBlocking<T>({
            val begin = System.currentTimeMillis()
            try {
               val result = runBlocking { block() }

               addBlockingTime(begin - System.currentTimeMillis())
               try {
                  it.complete(result)
               } catch (e: Throwable) {
                  // Ignore.
               }
            } catch (e: Throwable) {
               addBlockingTime(begin - System.currentTimeMillis())
               eventLoop.execute { it.fail(e) }
            }
         }, {
            if (it.failed()) {
               eventLoop.execute { cont.resumeWithException(it.cause()) }
            } else {
               eventLoop.execute { cont.resume(it.result()) }
            }
         })
      }

   /**
    *
    */
   protected suspend fun <T> javaBlocking(pool: WorkerPool, block: Supplier<T>): T =
      suspendCancellableCoroutine { cont ->
         pool.executeBlocking<T>({
            val begin = System.currentTimeMillis()
            try {
               val result = runBlocking { block.get() }

               addBlockingTime(begin - System.currentTimeMillis())
               try {
                  it.complete(result)
               } catch (e: Throwable) {
                  // Ignore.
               }
            } catch (e: Throwable) {
               addBlockingTime(begin - System.currentTimeMillis())
               eventLoop.execute { it.fail(e) }
            }
         }, {
            if (it.failed()) {
               eventLoop.execute { cont.resumeWithException(it.cause()) }
            } else {
               eventLoop.execute { cont.resume(it.result()) }
            }
         })
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

   suspend fun sleep(millis: Long) {
      delay(millis, TimeUnit.MILLISECONDS)
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
      if (MKernel.currentEventLoop === eventLoop) {
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
    * EventLoop has a WheelTimer with TICK_MS precision.
    * Do not use "delay" if a higher precision is needed.
    */
   override fun scheduleResumeAfterDelay(time: Long, unit: TimeUnit, continuation: CancellableContinuation<Unit>) {
      eventLoop.scheduleDelay(continuation, unit.toMillis(time))
   }

   /**
    *
    */
   override fun asSingle(): rx.Single<OUT> {
      return asSingle(this)
   }

   /*******************************************************************
    * Lifted FROM Deferred!!!
    *******************************************************************/


   override fun getCompleted(): OUT = getCompletedInternal() as OUT

   suspend override fun await(): OUT = awaitInternal() as OUT
   override val onAwait: SelectClause1<OUT>
      get() = this as SelectClause1<OUT>


   override fun onCancellation() {
      super.onCancellation()
   }

   override fun onParentCancellation(cause: Throwable?) {
      super.onParentCancellation(cause)
   }

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
      // Decrement inFlight
      eventLoop.decrementInFlight()

      // !!! Important !!! Ensure we clean up reference to Parent
      eventLoop.job = parent
      parent = null

      removeAllTimers()

      if (handle != null) {
         handle?.dispose()
         handle = null
      }

      end()

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

   inner class ActionMetrics {
      var tick: Boolean = true
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

      fun begin() {
         begin = if (tick) eventLoop.epochTick else System.currentTimeMillis()
      }

      fun end() = if (tick) eventLoop.epochTick - begin else System.currentTimeMillis() - begin

      fun blockingBegin() {
         blockingBegin = if (tick) eventLoop.epochTick else System.currentTimeMillis()
      }

      fun blockingEnd() {
         blocking += if (tick)
            eventLoop.epochTick - blockingBegin
         else
            System.currentTimeMillis() - blockingBegin
      }
   }
}

abstract class InternalAction<IN : Any, OUT : Any> : JobAction<IN, OUT>()

abstract class WorkerAction<IN : Any, OUT : Any> : JobAction<IN, OUT>()


/**
 *
 */
abstract class HttpAction : JobAction<RoutingContext, Unit>() {
   val req
      get() = request.request()

   val resp
      get() = request.response()

   protected fun param(name: String) =
      req.getParam(name)
}


class TryWhileException : RuntimeException()