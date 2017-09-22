package move.action

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.vertx.core.Handler
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.selects.SelectClause1
import kotlinx.coroutines.experimental.selects.SelectClause2
import move.threading.WorkerPool
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Supplier
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.ContinuationInterceptor
import kotlin.coroutines.experimental.CoroutineContext

//typealias ActorID = io.netty.util.AsciiString
//typealias ActorID = String

object LocalActorStore {
   val map = ConcurrentHashMap<ActorID, AbstractActorAction<*>>(10_000)

   fun get(id: ActorID) = map.get(id)
}


/**
 *
 */
class DuplicateActorIDException(
   val id: ActorID,
   val producer: ActorProducer<*, *>,
   val producer2: ActorProducer<*, *>
) : RuntimeException("ActorID [$id] not unique. Used for types [$producer] and [$producer2]")


/**
 *
 */
abstract class ActorProxy
<A : ActorAction, P : ActorProvider<A>, PRODUCER : ActorProducer<A, P>>(val producer: PRODUCER) {
   abstract val id: ActorID

   abstract fun send(message: ActorMessage)

   abstract infix fun <A : JobAction<IN, OUT>,
      IN : Any,
      OUT : Any,
      P : WorkerActionProvider<A, IN, OUT>> ask(message: AskMessage<A, IN, OUT, P>)
}

/**
 *
 */
class LocalActorProxy
<A : ActorAction, P : ActorProvider<A>, PRODUCER : ActorProducer<A, P>>(
   internal val actor: ActorAction, producer: PRODUCER) : ActorProxy<A, P, PRODUCER>(producer) {
   override val id: ActorID
      get() = actor.id

   val eventLoop get() = actor.eventLoop

   override fun send(message: ActorMessage) {
      actor.offer(message)
   }

   override infix fun <A : JobAction<IN, OUT>,
      IN : Any,
      OUT : Any,
      P : WorkerActionProvider<A, IN, OUT>> ask(message: AskMessage<A, IN, OUT, P>) {
      actor.offer(message)
   }
}

/**
 *
 */
class RemoteActorProxy
<A : ActorAction, P : ActorProvider<A>, PRODUCER : ActorProducer<A, P>>(
   override val id: ActorID,
   producer: PRODUCER) : ActorProxy<A, P, PRODUCER>(producer) {

   override fun send(message: ActorMessage) {
//      producer.send(id, message)
   }

   override infix fun <A : JobAction<IN, OUT>,
      IN : Any,
      OUT : Any,
      P : WorkerActionProvider<A, IN, OUT>> ask(message: AskMessage<A, IN, OUT, P>) {
      // Go through Broker.
//      producer.ask(id, message)
   }
}

/**
 *
 */
abstract class ActorProvider<A : ActorAction>(val provider: Provider<A>) {
   open val daemon = false

   abstract val actorClass: Class<A>
}


/**
 *
 */
abstract class ActorProducer<A : ActorAction, P : ActorProvider<A>>() {
   lateinit var provider: P
   abstract val actorClass: Class<A>

   @Inject
   protected fun inject(provider: P) {
      this.provider = provider
   }

   open operator fun invoke(id: String): ActorProxy<A, P, ActorProducer<A, P>> =
      invoke(ActorID(id))

   open operator fun invoke(id: ActorID): ActorProxy<A, P, ActorProducer<A, P>> {
      val actor = LocalActorStore.map.get(id)

      if (actor != null) {
         return LocalActorProxy(actor as A, this)
      } else {
         val newActor = LocalActorStore.map.computeIfAbsent(id, { provider.provider.get() })

         return LocalActorProxy(newActor as A, this)
//         return RemoteActorProxy(id, this)
      }
   }

//   fun <A : JobAction<IN, OUT>,
//      IN : Any,
//      OUT : Any,
//      P : WorkerActionProvider<A, IN, OUT>> ask(id: ActorID, message: AskMessage<A, IN, OUT, P>) {
//   }
//
//   fun send(id: ActorID, message: ActorMessage) {
//      invoke(id).send(message)
//   }
}

/**
 *
 */
abstract class DaemonProvider<T : ActorAction>(provider: Provider<T>) : ActorProvider<T>(provider) {
   override val daemon = true

   val sort: Int
   val role: NodeRole
   val name: String

   init {
      val annotation = actorClass.getAnnotation(Daemon::class.java)!!

      sort = annotation.order
      role = annotation.role

      if (annotation.value.isNotBlank()) {
         name = annotation.value.trim()
      } else {
         name = actorClass.canonicalName
      }
   }
}


/**
 *
 */
abstract class DaemonProducer<A : ActorAction, P : DaemonProvider<A>>()
   : ActorProducer<A, P>() {

   private var _instance: A? = null
   private var _proxy: LocalActorProxy<A, P, ActorProducer<A, P>>? = null

   val instance get() = _proxy!!

   operator fun invoke() = _proxy!!

   override operator fun invoke(id: ActorID): ActorProxy<A, P, ActorProducer<A, P>> {
      return _proxy!!
   }

   @Synchronized
   suspend fun start() {
      if (_instance != null) {
         return
      }

      _instance = provider.provider.get().apply {
         val id = ActorID(provider.name)

         // Launch
         launch(
            // Evenly spread daemons across all EventLoops
            MKernel.nextOrdered(),
            this@DaemonProducer,
            id
         ).await()?.apply { throw this }
      }
      _proxy = LocalActorProxy(
         _instance!!,
         this
      )
   }

   infix fun <A : JobAction<IN, OUT>,
      IN : Any,
      OUT : Any,
      P : WorkerActionProvider<A, IN, OUT>> ask(message: AskMessage<A, IN, OUT, P>) {
      _proxy?.ask(message)
   }

   infix fun <A : JobAction<IN, OUT>,
      IN : Any,
      OUT : Any,
      P : WorkerActionProvider<A, IN, OUT>> tell(message: AskMessage<A, IN, OUT, P>) {
      _proxy?.ask(message)
   }

   fun send(message: ActorMessage) {
      _proxy?.send(message)
   }

   companion object {
      private val daemonCounter = AtomicLong(0)
   }
}

/**
 *
 */
abstract class ActorMessage {
   open val name = javaClass.canonicalName

   open fun pack(): ByteBuf {
      return Unpooled.EMPTY_BUFFER
   }

   open fun pack(buffer: ByteBuf) {
   }
}

/**
 *
 */
data class ByteBufMessage(val buffer: ByteBuf) : ActorMessage() {
   override val name: String
      get() = "ByteBuf"

   override fun pack(): ByteBuf {
      return Unpooled.EMPTY_BUFFER
   }

   override fun pack(buffer: ByteBuf) {
      super.pack(buffer)
   }
}

const val ASK_ASYNC = 1
const val ASK_FIFO = 2

/**
 *
 */
data class AskMessage
<A : JobAction<IN, OUT>,
   IN : Any,
   OUT : Any,
   P : WorkerActionProvider<A, IN, OUT>>(
   val request: IN,
   val type: Int,
   val timeout: Long,
   val producer: WorkerActionProducer<A, IN, OUT, P>) : ActorMessage() {

   internal var callback: CompletableDeferred<OUT>? = null

   override val name: String
      get() = "Ask"

   suspend fun execute(): OUT {
      return producer.ask(request, timeout, TimeUnit.MILLISECONDS)
   }

   fun rxExecute(): DeferredAction<OUT> {
      return producer.rxAsk(request, timeout)
   }

   override fun pack(): ByteBuf {
      return super.pack()
   }

   override fun pack(buffer: ByteBuf) {
      super.pack(buffer)
   }
}

data class TimerMessage(val handle: TimerEventHandle) : ActorMessage()


const val TIMER_TYPE_INTERVAL = 0

abstract class ActorAction : AbstractActorAction<ActorMessage>() {
   open val intervalMillis: Long = 5000L
   open val intervalDelay: Boolean = true

   private var inFlight = 0

   suspend override fun beforeExecute() {
      startUp()

      if (intervalMillis > 0) {
         scheduleNextInterval()
      }
   }

   fun scheduleNextInterval() {
      // Schedule timer.
      if (isActive && !isCancelled) {
         eventLoop.scheduleTimer(TIMER_TYPE_INTERVAL, this, intervalMillis)
      }
   }

   suspend override fun afterExecute() {
      shutdown()
   }

   suspend override fun process(msg: ActorMessage) {
      when (msg) {
         is ByteBufMessage -> handleBuffer(msg)
         is AskMessage<*, *, *, *> -> handleAsk(msg)
         is TimerMessage -> handleTimer(msg)
         else -> handle(msg)
      }
   }

   fun onInterval(handle: TimerEventHandle) {
      if (handle.type == TIMER_TYPE_INTERVAL) {

      }
   }

   override fun onTimer(handle: TimerEventHandle) {
      offer(TimerMessage(handle))
   }

   suspend protected fun handleTimer(msg: TimerMessage) {
      if (msg.handle.type == TIMER_TYPE_INTERVAL) {
         wrapInterval()
      }
   }

   /**
    *
    */
   suspend protected fun handleBuffer(msg: ByteBufMessage) {
      // Inspect.
   }

   /**
    *
    */
   suspend protected fun handleAsk(msg: AskMessage<*, *, *, *>) {
      asyncAsk(msg)
   }

   suspend protected fun globalFifoAsk(msg: AskMessage<*, *, *, *>) {

   }

   suspend protected fun fifoAsk(msg: AskMessage<*, *, *, *>) {

   }

   protected fun asyncAsk(msg: AskMessage<*, *, *, *>) {
      inFlight++
      val future = msg.rxExecute()
      future.invokeOnCompletion {
         inFlight--
      }
   }

   /**
    *
    */
   suspend protected fun wrapInterval() {
      try {
         onInterval()
      } catch (e: Throwable) {
         try {
            onTimerException(e)
         } catch (e2: Throwable) {
         }
      }

      // Schedule next timer event.
      scheduleNextInterval()
   }

   suspend protected fun onTimerException(e: Throwable) {
      // Do nothing.
   }

   /**
    *
    */
   suspend open fun startUp() {}

   /**
    *
    */
   suspend open fun shutdown() {}

   /**
    *
    */
   suspend open fun onInterval() {}

   /**
    *
    */
   suspend open fun handle(msg: ActorMessage) {

   }
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

   lateinit var id: ActorID
   var created = 0L
   var ended = 0L
   private var starting = true
   private var uncaughtExceptionCount = 0L

   private var _onStarted: CompletableDeferred<Throwable?>? = null
   lateinit var eventLoop: MEventLoop
   lateinit var producer: ActorProducer<*, *>
   lateinit var _channel: Channel<E>
   override val channel: Channel<E>
      get() = _channel

   @Suppress("LeakingThis")
   override val context: CoroutineContext = this
   override val coroutineContext: CoroutineContext get() = context

   override val hasCancellingState: Boolean get() = true

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
      begin = if (tick)
         eventLoop.epochTick
      else
         System.currentTimeMillis()
   }

   internal fun end() {
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

   private fun removeAllTimers() {
      _timers?.apply { forEach { it.remove() }; clear() }
      _timers = null
   }

   override fun onTimer(handle: TimerEventHandle) {
   }

   /**
    * Launches action as the root coroutine.
    */
   internal open fun launch(
      eventLoop: MEventLoop,
      producer: ActorProducer<*, *>,
      id: ActorID): CompletableDeferred<Throwable?> {

      this._onStarted = CompletableDeferred()
      this.id = id
      this.producer = producer
      this.eventLoop = eventLoop
      this.created = System.currentTimeMillis()
//      this.created = eventLoop.epoch

      try {
         LocalActorStore.map.put(id, this)
         if (MKernel.currentEventLoop !== eventLoop) {
            eventLoop.execute {
               CoroutineStart.DEFAULT({ internalExecute() }, this, this)
            }
         } else {
            CoroutineStart.DEFAULT({ internalExecute() }, this, this)
         }
      } catch (e: Throwable) {
         LocalActorStore.map.remove(id)
         throw e
      }

      return _onStarted!!
   }

   suspend protected open fun beforeExecute() {
   }

   suspend protected open fun afterExecute() {
   }

   suspend internal fun internalExecute() {
      begin()
      try {
         try {
            // Create channel.
            _channel = createChannel()

            // Invoke beforeExecute()
            beforeExecute()

            _onStarted?.complete(null)
            _onStarted = null
         } catch (e: Throwable) {
            _channel.close(e)
            // Ignore.
            _onStarted?.complete(e)
            _onStarted = null
            return
         }

         // Iterate over messages in the channel.
         for (msg in _channel) {
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

         try {
            if (!_channel.isClosedForSend || !_channel.isClosedForReceive) {
               _channel.close()
            }
         } catch (e: Throwable) {
         }

         afterExecute()
      } finally {
         try {
            // Remove timers.
            removeAllTimers()
         } finally {
            try {
               end()
            } finally {
               // Remove from LocalActorStore
               LocalActorStore.map.remove(id)
            }
         }
      }
   }

   suspend protected fun createChannel(): Channel<E> {
      return LinkedListChannel()
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
         if (MKernel.currentEventLoop !== eventLoop) {
            // Run it on the correct event loop.
            eventLoop.execute {
               continuation.resume(value)
            }
         } else {
            continuation.resume(value)
         }
      }

      override fun resumeWithException(exception: Throwable) {
         if (MKernel.currentEventLoop !== eventLoop) {
            // Run it on the correct event loop.
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
      return ActorContinuation(continuation)
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
    * EventLoop has a WheelTimer with 100ms precision.
    * Do not use "delay" if nanosecond precision is needed.
    */
   override fun scheduleResumeAfterDelay(time: Long,
                                         unit: TimeUnit,
                                         continuation: CancellableContinuation<Unit>) {
      eventLoop.scheduleDelay(continuation, unit.toMillis(time))
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
}