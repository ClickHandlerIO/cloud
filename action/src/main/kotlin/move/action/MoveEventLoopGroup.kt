package move.action

import io.netty.channel.EventLoop
import io.vertx.core.Context
import io.vertx.core.Vertx
import io.vertx.core.impl.ContextImpl
import io.vertx.core.impl.VertxInternal
import io.vertx.core.json.JsonObject
import java.util.concurrent.Executors
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

/**
 *
 */
class MoveEventLoopGroup(private val vertxInternal: VertxInternal) {
   val eventLoopDefault = vertxInternal.createEventLoopContext(null, null, JsonObject(), null)
   val executors = vertxInternal.eventLoopGroup
      .iterator()
      .asSequence()
      .map { MoveEventLoop(eventLoopDefault, vertxInternal, JsonObject(), it as EventLoop) }
      .toList()
   val executorsByEventLoop = executors.map { it.eventLoop to it }.toMap()

   // Application wide scheduled executor.
   // This is used to process ticks for each MoveEventLoop.
   // Each MoveEventLoop has it's own structures and WheelTimers to
   // invoke and resume Actions that are assigned to it.
   // This allows for a very scalable and predictable behavior when managing
   // large amounts of Timers. Millions of timers can effectively be managed
   // with minimal overhead at the cost of loss of precision.
   val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()

   init {
      // Check for timeouts every 100ms
      scheduledExecutor.scheduleAtFixedRate(
         { executors.forEach { it.tick() } },
         MoveEventLoop.TICK_MS,
         MoveEventLoop.TICK_MS,
         TimeUnit.MILLISECONDS
      )

      vertxInternal.addCloseHook {
         scheduledExecutor.shutdownNow()
      }
   }

   fun nextRandom() = executors[ThreadLocalRandom.current().nextInt(0, executors.size)]

   fun next(): MoveEventLoop {
      return next(io.vertx.core.Vertx.currentContext())
   }

   fun nextBiased(context: Context?): MoveEventLoop {
      return nextRandom()
   }

   fun next(context: Context?): MoveEventLoop {
      if (context == null)
         return nextRandom()

      val eventLoop = (context as ContextImpl).nettyEventLoop() ?: return nextRandom()

      return executorsByEventLoop[eventLoop] ?: nextRandom()
   }

   companion object {
      val instances = mutableMapOf<Vertx, MoveEventLoopGroup>()

      val currentEventLoop get() = local.get()

      internal val local = ThreadLocal<MoveEventLoop>()

      fun setLocal(eventLoop: MoveEventLoop) {
         local.set(eventLoop)
      }

      @Synchronized
      fun get(vertx: Vertx): MoveEventLoopGroup {
         val i = instances[vertx]
         if (i != null) {
            return i
         }

         val n = MoveEventLoopGroup(vertx as VertxInternal)
         instances[vertx] = n
         return n
      }

      fun get(vertx: io.vertx.rxjava.core.Vertx): MoveEventLoopGroup {
         return get(vertx.delegate)
      }

      fun get(vertxInternal: VertxInternal): MoveEventLoopGroup {
         return get(vertxInternal as Vertx)
      }
   }
}
