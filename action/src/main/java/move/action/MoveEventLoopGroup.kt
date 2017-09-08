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
   val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()

   init {
      // Check for timeouts every 100ms
      scheduledExecutor.scheduleAtFixedRate(
         { executors.forEach { it.tick() } },
         MoveEventLoop.TICK,
         MoveEventLoop.TICK,
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
