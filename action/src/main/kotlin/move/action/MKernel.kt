package move.action

import io.netty.channel.EventLoop
import io.vertx.core.Context
import io.vertx.core.Vertx
import io.vertx.core.impl.ContextImpl
import io.vertx.core.impl.VertxInternal
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.*
import move.hash.CRC16
import java.lang.management.ManagementFactory
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext

// EventLoop thread local.
val CURRENT_LOOP = ThreadLocal<MEventLoop>()

/**
 * Lifts Vert.x event loops into MEventLoop. Every Action and Actor
 * is launched on an event loop. Blocking code may be ran from any
 * Action or Actor.
 */
object MKernel {
   inline val currentEventLoop get() = CURRENT_LOOP.get()

   // VertxInternal
   val vertxInternal = VERTX.delegate as VertxInternal

   // EventLoop default.
   private val eventLoopDefault = vertxInternal.createEventLoopContext(
      null,
      null,
      JsonObject(),
      null
   )

   // Build EventLoops.
   val eventLoops = vertxInternal.eventLoopGroup
      .iterator()
      .asSequence()
      .map {
         MEventLoop(
            eventLoopDefault,
            vertxInternal,
            JsonObject(),
            it as EventLoop
         )
      }
      .toList()
      .toTypedArray()

   // Number of event loops.
   val eventLoopCount = eventLoops.size

   // By Netty EventLoop.
   val executorsByEventLoop = eventLoops.map { it.eventLoop to it }.toMap()

   // Application wide scheduled executor.
   // This is used to process ticks for each MEventLoop.
   // Each MEventLoop has it's own structures and WheelTimers to
   // invoke and resume Actions that are assigned to it.
   // This allows for a very scalable and predictable behavior when managing
   // large amounts of Timers. Millions of timers can effectively be managed
   // with minimal overhead at the cost of loss of precision.
   // This same thread is also used to gather JVM metrics from EventLoops
   // WorkerPools, etc.
   val scheduledExecutor = Executors.newSingleThreadScheduledExecutor(object : ThreadFactory {
      override fun newThread(r: Runnable?): Thread {
         val thread = Thread(r, "timer")
         thread.isDaemon = true
         return thread
      }
   })

   // Thread and CPU monitoring.
   val threadMX = ManagementFactory.getThreadMXBean()

   // Blocking threads.

   // Memory Management monitoring.
   val memoryMX = ManagementFactory.getMemoryMXBean()

   // Garbage collection monitoring
   val garbageMX = ManagementFactory.getGarbageCollectorMXBeans()

   // Tick count.
   val ticks = AtomicLong(0L)

   val counter = AtomicLong(0L)

   init {
      // Setup event loops and populate thread local.
      eventLoops.forEach { it.init { CURRENT_LOOP.set(it) } }

      // Check for timeouts every TICK_MS
      scheduledExecutor.scheduleAtFixedRate(
         this::tick,
         MEventLoop.TICK_MS,
         MEventLoop.TICK_MS,
         TimeUnit.MILLISECONDS
      )

      vertxInternal.addCloseHook {
         scheduledExecutor.shutdownNow()
      }
   }

   /**
    * Dummy method to ensure singleton is created.
    */
   fun init() {}

   private fun tick() {
      ticks.incrementAndGet()

      // Determine event loops.
      eventLoops.forEach {
         val cpuTime = threadMX.getThreadCpuTime(it.id)
         val userTime = threadMX.getThreadUserTime(it.id)

//         if (ticks.get() % 25 == 0L) {
//            println("Thread: ${it.id} CPU: $cpuTime")
//         }

         // Tick
         it.tick()
      }
   }

   fun forKey(key: ByteArray) = eventLoops[CRC16.calc(key) % eventLoopCount]

   fun forKey(key: String) = eventLoops[CRC16.calc(key) % eventLoopCount]

   fun forKey(key: ActorID) = eventLoops[key.hashCode() % eventLoopCount]

   //   fun forKey(key: AsciiString): MEventLoop {
//      return eventLoops[key.hashCode() % eventLoopCount]
//   }

   /**
    *
    */
   fun next(): MEventLoop {
      return next(io.vertx.core.Vertx.currentContext())
   }

   /**
    *
    */
   fun next(context: Context?): MEventLoop {
      if (context == null)
         return nextRandom()

      val eventLoop = (context as ContextImpl).nettyEventLoop() ?: return nextRandom()

      return executorsByEventLoop[eventLoop] ?: nextRandom()
   }

   fun nextOrdered() = eventLoops[(counter.getAndIncrement() % eventLoopCount).toInt()]

   /**
    *
    */
   fun nextRandom() = eventLoops[ThreadLocalRandom.current().nextInt(0, eventLoops.size)]

   /**
    *
    */
   fun getOrCreateContext() = CURRENT_LOOP.get() ?: nextRandom()

   /**
    *
    */
   fun forEachExecute(action: (MEventLoop) -> Unit) {
      eventLoops.forEach { it.execute { action(it) } }
   }

   /**
    *
    */
   fun forEach(action: (MEventLoop) -> Unit) {
      eventLoops.forEach(action)
   }

   /**
    *
    */
   suspend fun initEventLoops(block: suspend (MEventLoop) -> Unit) {
      eventLoops.forEach {
         async(Unconfined) {
            block(it)
         }.await()
      }
   }

   /**
    *
    */
   fun initEventLoopsBlocking(block: suspend (MEventLoop) -> Unit) {
      runBlocking {
         eventLoops.forEach {
            async(Unconfined) {
               block(it)
            }.await()
         }
      }
   }
}

/**
 *
 */
class MEventLoopDispatcher(val eventLoop: MEventLoop) : CoroutineDispatcher(), Delay {

   fun execute(task: () -> Unit) = eventLoop.execute(task)

   override fun dispatch(context: CoroutineContext,
                         block: Runnable) {

      if (Vertx.currentContext() === eventLoop) {
         block.run()
      } else {
         execute {
            block.run()
         }
      }
   }

   override fun scheduleResumeAfterDelay(time: Long,
                                         unit: TimeUnit,
                                         continuation: CancellableContinuation<Unit>) {

      eventLoop.scheduleDelay(
         continuation,
         unit.toMillis(time)
      )
   }

   override fun invokeOnTimeout(time: Long,
                                unit: TimeUnit,
                                block: Runnable): DisposableHandle {

      return if (Vertx.currentContext() === eventLoop) {
         // Run directly.
         block.run()
         // Already disposed.
         EmptyDisposableHandle
      } else {
         val handle = MoveEventLoopDisposableHandle(block)
         // Needs to run on EventLoop.
         eventLoop.execute { handle.block?.run() }
         // Return disposable handle.
         handle
      }
   }

   /**
    *
    */
   inner class ContinuationImpl<in T>(
      val action: JobAction<*, *>?,
      private val continuation: Continuation<T>) : Continuation<T> {

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
      return ContinuationImpl(eventLoop.job, continuation)
   }
}