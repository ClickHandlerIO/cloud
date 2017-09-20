package move.action

import io.netty.buffer.ByteBuf
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.selects.SelectClause1
import kotlinx.coroutines.experimental.selects.SelectClause2
import move.threading.WorkerPool
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.function.Supplier
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.ContinuationInterceptor
import kotlin.coroutines.experimental.CoroutineContext

class ActorProvider<E> {

}

class ActorProducer<E, P : ActorProvider<E>> {

}

abstract class ActorMessage {
   open val name = javaClass.canonicalName
}

data class ByteBufMessage(val buffer: ByteBuf) : ActorMessage() {
   override val name: String
      get() = "ByteBuf"
}

data class ActionAskMessage(val request: Any,
                            val producer: ActionProducer<*, *, *, *>,
                            val response: (Any) -> Unit) : ActorMessage() {
   override val name: String
      get() = "Ask"
}


/**
 * Number
 */
data class ActorFrame(val reqId: String,
                      val id: String,
                      val seq: Int = 0,
                      val size: Int = 0,
                      val needsAck: Boolean = true) {

   companion object {
      fun unpack(buf: ByteBuf) {
         // <ActorFrame> reqId someId -1 9000
      }

   }
}

object ActorPacker {
   fun pack(id: String) {

   }
}

data class TimerMessage(val handle: TimerHandle) : ActorMessage()

abstract class ActorAction : AbstractActorAction<ActorMessage>() {
   open val millis: Long = 5000L
   private var timerHandle: TimerHandle? = null

   suspend override fun beforeExecute() {
      startUp()

      if (millis > 0) {
         scheduleTimer()
      }
   }

   fun scheduleTimer() {
      // Schedule timer.
      if (isActive && !isCancelled) {
         eventLoop.scheduleTimer(this, millis)
      }
   }

   suspend override fun afterExecute() {
      timerHandle?.remove()
      timerHandle = null

      shutdown()
   }

   suspend override fun process(msg: ActorMessage) {
      // Force the timer event if necessary.
      // If a queue is bounded then it's possible to
      // starve the actor of it's timer message.
      if (timerHandle != null) {
         wrapTimerRun(TimerMessage(timerHandle!!))
         timerHandle = null
      }

      when (msg) {
         is TimerMessage -> wrapTimerRun(msg)
         is ByteBufMessage -> onByteBuf(msg)
         is ActionAskMessage -> onAsk(msg)
         else -> handle(msg)
      }
   }

   /**
    *
    */
   override fun onTimerEvent(handle: TimerHandle) {
      // Add to queue.
      try {
         offer(TimerMessage(handle))
      } catch (e: Throwable) {
         timerHandle = handle
         // Maybe the queue is full
         scheduleTimer()
      }
   }

   /**
    *
    */
   suspend protected fun onByteBuf(msg: ByteBufMessage) {
      // Inspect.

   }

   /**
    *
    */
   suspend protected fun onAsk(msg: ActionAskMessage) {

   }

   /**
    *
    */
   suspend protected fun wrapTimerRun(msg: TimerMessage) {
      try {
         onTimer(msg)
      } catch (e: Throwable) {
         try {
            onTimerException(e)
         } catch (e2: Throwable) {
         }
      }

      // Schedule next timer event.
      scheduleTimer()
   }

   suspend protected fun onTimerException(e: Throwable) {
      // Do nothing.
   }

   /**
    *
    */
   suspend abstract fun startUp()

   /**
    *
    */
   suspend abstract fun shutdown()

   /**
    *
    */
   suspend abstract fun onTimer(msg: TimerMessage)

   /**
    *
    */
   suspend abstract fun handle(msg: ActorMessage)
}

abstract class AbstractActorAction<E> :
   JobSupport(true),
   ActorScope<E>,
   ActorJob<E>,
   Channel<E>,
   Delay,
   Continuation<Unit>,
   ContinuationInterceptor,
   HasTimers {

   private var hasTimers = false
   private var _timers: ArrayList<TimerHandle>? = null
   internal val timers
      get() = _timers!!

   lateinit var id: String
   var created = 0L
   var ended = 0L
   private var uncaughtExceptionCount = 0L

   lateinit var eventLoop: MoveEventLoop
   lateinit var _channel: Channel<E>
   override val channel: Channel<E>
      get() = _channel

   @Suppress("LeakingThis")
   override val context: CoroutineContext = this
   override val coroutineContext: CoroutineContext get() = context

   override val hasCancellingState: Boolean get() = true

   protected var tick = true

   var begin: Long = 0
   var beginTick: Long = 0
   var endTick: Long = 0
   var cpu: Long = 0
   var cpuBegin: Long = 0
   var blocking: Long = 0
   var blockingBegin: Long = 0

   var shortCircuited = false
   var fallbackSucceed = false
   var fallbackFailed = false
   var failed = false

   internal fun begin() {
      begin = if (tick) eventLoop.epochTick else System.currentTimeMillis()
   }

   internal fun end() = if (tick) eventLoop.epochTick - begin else System.currentTimeMillis() - begin

   internal fun blockingBegin() {
      blockingBegin = if (tick) eventLoop.epochTick else System.currentTimeMillis()
   }

   internal fun blockingEnd() {
      blocking += if (tick)
         eventLoop.epochTick - blockingBegin
      else
         System.currentTimeMillis() - blockingBegin
   }

   override fun addTimer(handle: TimerHandle) {
      hasTimers = true
      if (_timers == null) {
         _timers = arrayListOf(handle)
      }
   }

   override fun removeTimer(handle: TimerHandle) {
      if (_timers != null)
         _timers!! -= handle
      hasTimers = !timers.isEmpty()
      if (!hasTimers)
         _timers = null
   }

   protected open fun onTimerEvent(handle: TimerHandle) {
   }

   /**
    * Launches action as the root coroutine.
    */
   internal open fun launch(eventLoop: MoveEventLoop,
      //                            provider: ActionProvider<*, IN, OUT>,
                            id: String,
                            timeoutTicks: Long = 0,
                            root: Boolean = false) {
//      if (MoveThreadManager.currentEventLoop !== eventLoop) {
//         throw RuntimeException("Invoked from outside EventLoop thread.")
//      }

//      this.metrics = ActionMetrics()
      this.id = id
      this.eventLoop = eventLoop
      this.created = System.currentTimeMillis()
      this.created = eventLoop.epoch
      this._channel = LinkedListChannel()

      CoroutineStart.DEFAULT({ internalExecute() }, this, this)
   }

   suspend protected open fun beforeExecute() {
   }

   suspend protected open fun afterExecute() {
   }

   suspend internal fun internalExecute() {
      beforeExecute()

      // Iterate over messages in the channel.
      for (msg in channel) {
         try {
            process(msg)
         } catch (e: Throwable) {
            uncaughtExceptionCount++

            try {
               onProcessException(msg, e)
            } catch (e2: Throwable) {
               uncaughtExceptionCount++
            }
         }
      }

      // Remove timers.
      if (hasTimers) {
         timers.forEach { it.remove() }
         timers.clear()
      }

      ended = System.currentTimeMillis()

      afterExecute()
   }

   suspend protected abstract fun process(msg: E)

   suspend protected open fun onProcessException(msg: E, e: Throwable) {
   }

   override fun resume(value: Unit) {
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

   override val isClosedForReceive: Boolean
      get() = _channel.isClosedForReceive
   override val isEmpty: Boolean
      get() = _channel.isEmpty
   override val onReceive: SelectClause1<E>
      get() = _channel.onReceive
   override val onReceiveOrNull: SelectClause1<E?>
      get() = _channel.onReceiveOrNull

   override fun iterator(): ChannelIterator<E> {
      return _channel.iterator()
   }

   override fun poll(): E? {
      return _channel.poll()
   }

   suspend override fun receive(): E {
      return _channel.receive()
   }

   suspend override fun receiveOrNull(): E? {
      return _channel.receiveOrNull()
   }

   override val isClosedForSend: Boolean
      get() = _channel.isClosedForSend
   override val isFull: Boolean
      get() = _channel.isFull
   override val onSend: SelectClause2<E, SendChannel<E>>
      get() = _channel.onSend

   override fun close(cause: Throwable?): Boolean {
      return _channel.close(cause)
   }

   override fun offer(element: E): Boolean {
      return _channel.offer(element)
   }

   suspend override fun send(element: E) {
      _channel.send(element)
   }


   /**
    *
    */
   inner class ActorContinuation<T>(val continuation: Continuation<T>) : Continuation<T> {
      override val context: CoroutineContext
         get() = continuation.context

      override fun resume(value: T) {
         if (MoveThreadManager.currentEventLoop !== eventLoop) {
            eventLoop.execute {
               continuation.resume(value)
            }
         } else {
            continuation.resume(value)
         }
      }

      override fun resumeWithException(exception: Throwable) {
         if (MoveThreadManager.currentEventLoop !== eventLoop) {
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
      val actorContinuation = ActorContinuation(continuation)
      return actorContinuation
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
      if (MoveThreadManager.currentEventLoop === eventLoop) {
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
   protected suspend fun <T> blocking(block: suspend () -> T): T =
      suspendCancellableCoroutine { cont ->
         eventLoop.executeBlocking0<T>({
            blockingBegin()
            try {
               val result = runBlocking { block() }
               it.complete(result)
            } catch (e: Throwable) {
               eventLoop.execute { it.fail(e) }
            }
         }, {
            blockingEnd()
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
         eventLoop.executeBlocking0<T>({
            blockingBegin()
            try {
               val result = runBlocking { block.get() }
               it.complete(result)
            } catch (e: Throwable) {
               eventLoop.execute { it.fail(e) }
            }
         }, {
            blockingEnd()
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
               blockingBegin()
               try {
                  val result = runBlocking {
                     try {
                        block.invoke()
                     } catch (e: Throwable) {
                        throw e
                     }
                  }
                  blockingEnd()
                  eventLoop.execute {
                     cont.resume(result)
                  }
               } catch (e: Throwable) {
                  blockingEnd()
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
               blockingBegin()
               try {
                  val result = runBlocking {
                     try {
                        block.get()
                     } catch (e: Throwable) {
                        throw e
                     }
                  }
                  blockingEnd()
                  eventLoop.execute {
                     cont.resume(result)
                  }
               } catch (e: Throwable) {
                  blockingEnd()
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
            blockingBegin()
            try {
               val result = runBlocking { block() }
               it.complete(result)
            } catch (e: Throwable) {
               it.fail(e)
            }
         }, {
            blockingEnd()
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
            blockingBegin()
            try {
               val result = runBlocking { block.get() }
               blockingEnd()
               it.complete(result)
            } catch (e: Throwable) {
               blockingEnd()
               it.fail(e)
            }
         }, {
            if (it.failed()) {
               eventLoop.execute { cont.resumeWithException(it.cause()) }
            } else {
               eventLoop.execute { cont.resume(it.result()) }
            }
         })
      }

   inner class Metrics {
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