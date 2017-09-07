package move.action

import io.netty.channel.EventLoop
import io.vertx.core.Context
import io.vertx.core.Vertx
import io.vertx.core.impl.ContextImpl
import io.vertx.core.impl.VertxInternal
import io.vertx.core.json.JsonObject
import java.util.concurrent.ThreadLocalRandom

/**
 *
 */
class ActionEventLoopGroup(private val vertxInternal: VertxInternal) {
   val eventLoopDefault = vertxInternal.createEventLoopContext(null, null, JsonObject(), null)
   val executors = vertxInternal.eventLoopGroup
      .iterator()
      .asSequence()
      .map { ActionEventLoopContext(eventLoopDefault, vertxInternal, JsonObject(), it as EventLoop) }
      .toList()
   val executorsByEventLoop = executors.map { it.eventLoop to it }.toMap()

   private var timerID: Long = 0L

   init {
      // Check for timeouts every 200ms
      timerID = vertxInternal.setPeriodic(ActionEventLoopContext.FREQUENCY_ADJUSTMENT) {
         executors.forEach {
            it.processTimeouts()
         }
      }

      vertxInternal.addCloseHook {
         vertxInternal.cancelTimer(timerID)
      }
   }

   fun nextRandom() = executors[ThreadLocalRandom.current().nextInt(0, executors.size)]

   fun next(): ActionEventLoopContext {
      return next(io.vertx.core.Vertx.currentContext())
   }

   fun nextBiased(context: Context?): ActionEventLoopContext {
      return nextRandom()
   }

   fun next(context: Context?): ActionEventLoopContext {
      if (context == null)
         return nextRandom()

      val eventLoop = (context as ContextImpl).nettyEventLoop() ?: return nextRandom()

      return executorsByEventLoop[eventLoop] ?: nextRandom()
   }

   companion object {
      val instances = mutableMapOf<Vertx, ActionEventLoopGroup>()

      @Synchronized
      fun get(vertx: Vertx): ActionEventLoopGroup {
         val i = instances[vertx]
         if (i != null) {
            return i
         }

         val n = ActionEventLoopGroup(vertx as VertxInternal)
         instances[vertx] = n
         return n
      }

      fun get(vertx: io.vertx.rxjava.core.Vertx): ActionEventLoopGroup {
         return get(vertx.delegate)
      }

      fun get(vertxInternal: VertxInternal): ActionEventLoopGroup {
         return get(vertxInternal as Vertx)
      }
   }
}
