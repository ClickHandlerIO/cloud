package move.action

import io.netty.channel.EventLoop
import io.vertx.core.Context
import io.vertx.core.impl.ContextImpl
import io.vertx.core.impl.VertxInternal
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.async
import move.hash.CRC16
import java.lang.management.ManagementFactory
import java.util.concurrent.Executors
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

val CURRENT_LOOP = ThreadLocal<MoveEventLoop>()

/**
 *
 */
object MoveKernel {
   inline val currentEventLoop get() = CURRENT_LOOP.get()

   val vertxInternal = VERTX.delegate as VertxInternal
   val eventLoopDefault = vertxInternal.createEventLoopContext(
      null,
      null,
      JsonObject(),
      null
   )

   // Build EventLoops.
   val eventLoops = vertxInternal.eventLoopGroup
      .iterator()
      .asSequence()
      .map { MoveEventLoop(eventLoopDefault, vertxInternal, JsonObject(), it as EventLoop) }
      .toList()

   val eventLoopCount = eventLoops.size

   // By Netty EventLoop.
   val executorsByEventLoop = eventLoops.map { it.eventLoop to it }.toMap()

   // Application wide scheduled executor.
   // This is used to process ticks for each MoveEventLoop.
   // Each MoveEventLoop has it's own structures and WheelTimers to
   // invoke and resume Actions that are assigned to it.
   // This allows for a very scalable and predictable behavior when managing
   // large amounts of Timers. Millions of timers can effectively be managed
   // with minimal overhead at the cost of loss of precision.
   // This same thread is also used to gather JVM metrics from EventLoops
   // WorkerPools, etc.
   val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()

   // Thread monitoring.
   val threadMX = ManagementFactory.getThreadMXBean()

   // Blocking threads.

   // Memory Management monitoring.
   val memoryMX = ManagementFactory.getMemoryMXBean()

   val ticks = AtomicLong(0L)

   init {
      eventLoops.forEach { it.init { CURRENT_LOOP.set(it) } }

      // Check for timeouts every TICK_MS
      scheduledExecutor.scheduleAtFixedRate(
         this::tick,
         MoveEventLoop.TICK_MS,
         MoveEventLoop.TICK_MS,
         TimeUnit.MILLISECONDS
      )

      vertxInternal.addCloseHook {
         scheduledExecutor.shutdownNow()
      }
   }

   fun init() {}

   private fun tick() {
      ticks.incrementAndGet()

      // Determine event loops.
      eventLoops.forEach {
         val cpuTime = threadMX.getThreadCpuTime(it.id)
         val userTime = threadMX.getThreadUserTime(it.id)

         if (ticks.get() % 25 == 0L) {
            println("Thread: ${it.id} CPU: $cpuTime")
         }

         // Tick
         it.tick()
      }
   }

   fun forKey(bytes: ByteArray): MoveEventLoop {
      return eventLoops[CRC16.calc(bytes) % eventLoopCount]
   }

   /**
    *
    */
   fun next(): MoveEventLoop {
      return next(io.vertx.core.Vertx.currentContext())
   }

   /**
    *
    */
   fun next(context: Context?): MoveEventLoop {
      if (context == null)
         return nextRandom()

      val eventLoop = (context as ContextImpl).nettyEventLoop() ?: return nextRandom()

      return executorsByEventLoop[eventLoop] ?: nextRandom()
   }

   /**
    *
    */
   fun nextRandom() = eventLoops[ThreadLocalRandom.current().nextInt(0, eventLoops.size)]

   fun getOrCreateContext() = CURRENT_LOOP.get() ?: nextRandom()

   fun forEachExecute(action: (MoveEventLoop) -> Unit) {
      eventLoops.forEach { it.execute { action(it) } }
   }

   fun forEach(action: (MoveEventLoop) -> Unit) {
      eventLoops.forEach(action)
   }

   suspend fun initEventLoops(block: suspend (MoveEventLoop) -> Unit) {
      eventLoops.forEach {
         async(Unconfined) {
            block(it)
         }.await()
      }
   }

//   companion object {
//      val instances = mutableMapOf<Vertx, MoveKernel>()
//
//      val currentEventLoop get() = local.get()
//
//      internal val local = ThreadLocal<MoveEventLoop>()
//
//      protected fun setLocal(eventLoop: MoveEventLoop) {
//         local.set(eventLoop)
//      }
//
//      @Synchronized
//      fun get(vertx: Vertx): MoveKernel {
//         val i = instances[vertx]
//         if (i != null) {
//            return i
//         }
//
//         val n = MoveKernel(vertx as VertxInternal)
//         instances[vertx] = n
//         return n
//      }
//
//      fun get(vertx: io.vertx.rxjava.core.Vertx): MoveKernel {
//         return get(vertx.delegate)
//      }
//
//      fun get(vertxInternal: VertxInternal): MoveKernel {
//         return get(vertxInternal as Vertx)
//      }
//   }
}
