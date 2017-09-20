package move.action

import io.netty.channel.EventLoop
import io.vertx.core.Context
import io.vertx.core.Vertx
import io.vertx.core.impl.ContextImpl
import io.vertx.core.impl.VertxInternal
import io.vertx.core.json.JsonObject
import java.lang.management.ManagementFactory
import java.util.concurrent.Executors
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 *
 */
class MoveThreadManager(private val vertxInternal: VertxInternal) {
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

   companion object {
      val instances = mutableMapOf<Vertx, MoveThreadManager>()

      val currentEventLoop get() = local.get()

      internal val local = ThreadLocal<MoveEventLoop>()

      fun setLocal(eventLoop: MoveEventLoop) {
         local.set(eventLoop)
      }

      @Synchronized
      fun get(vertx: Vertx): MoveThreadManager {
         val i = instances[vertx]
         if (i != null) {
            return i
         }

         val n = MoveThreadManager(vertx as VertxInternal)
         instances[vertx] = n
         return n
      }

      fun get(vertx: io.vertx.rxjava.core.Vertx): MoveThreadManager {
         return get(vertx.delegate)
      }

      fun get(vertxInternal: VertxInternal): MoveThreadManager {
         return get(vertxInternal as Vertx)
      }
   }
}
