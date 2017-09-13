package move.action

import io.vertx.ext.web.RoutingContext
import io.vertx.rxjava.core.Vertx
import kotlinx.coroutines.experimental.Deferred
import rx.Single

/**
 *
 */
val LOCAL_BROKER = LocalBroker

var _GATEWAY_BROKER: ActionBroker = LOCAL_BROKER

fun changeGatewayBroker(broker: ActionBroker) {
   _GATEWAY_BROKER = broker
}

/**
 * Worker Actions are all defaulted to go through the Gateway Broker.
 */
val GATEWAY_BROKER
   get() = _GATEWAY_BROKER

/**
 * In charge of creating, scheduling. queuing, logging, and invoking Actions.
 *
 * Each node has a single queue
 */
class KernelBroker(val vertx: Vertx) {
   val eventLoopGroup = MoveEventLoopGroup.get(vertx)
   val eventLoops = eventLoopGroup.executors


}

/**
 * In charge of delivering to an ActionKernel
 */
abstract class ActionBroker {
   abstract val id: Int

   /**
    *
    */
   abstract suspend fun <A : InternalAction<IN, OUT>, IN : Any, OUT : Any> ask(
      request: IN,
      provider: InternalActionProvider<InternalAction<IN, OUT>, IN, OUT>,
      timeoutTicks: Long = 0
   ): OUT

   /**
    *
    */
   abstract suspend fun <A : InternalAction<IN, OUT>, IN : Any, OUT : Any> ask(
      request: IN,
      provider: InternalActionProvider<InternalAction<IN, OUT>, IN, OUT>,
      timeoutTicks: Long = 0,
      delaySeconds: Int = 0
   ): OUT

   /**
    *
    */
   abstract fun <A : InternalAction<IN, OUT>, IN : Any, OUT : Any> rxAsk(
      request: IN,
      provider: InternalActionProvider<InternalAction<IN, OUT>, IN, OUT>,
      timeoutTicks: Long = 0,
      delaySeconds: Int = 0,
      root: Boolean = false
   ): DeferredAction<OUT>


   /**
    *
    */
   abstract fun <A : InternalAction<IN, OUT>, IN : Any, OUT : Any> launch(
      request: IN,
      provider: InternalActionProvider<InternalAction<IN, OUT>, IN, OUT>,
      timeoutTicks: Long = 0,
      delaySeconds: Int = 0,
      root: Boolean = false
   ): DeferredAction<OUT>






   /**
    *
    */
   abstract suspend fun <A : WorkerAction<IN, OUT>, IN : Any, OUT : Any> ask(
      request: IN,
      provider: WorkerActionProvider<WorkerAction<IN, OUT>, IN, OUT>,
      timeoutTicks: Long = 0
   ): OUT

   /**
    *
    */
   abstract suspend fun <A : WorkerAction<IN, OUT>, IN : Any, OUT : Any> ask(
      request: IN,
      provider: WorkerActionProvider<WorkerAction<IN, OUT>, IN, OUT>,
      timeoutTicks: Long = 0,
      delaySeconds: Int = 0
   ): OUT

   /**
    *
    */
   abstract fun <A : WorkerAction<IN, OUT>, IN : Any, OUT : Any> rxAsk(
      request: IN,
      provider: WorkerActionProvider<WorkerAction<IN, OUT>, IN, OUT>,
      timeoutTicks: Long = 0,
      delaySeconds: Int = 0,
      root: Boolean = false
   ): DeferredAction<OUT>

   /**
    *
    */
   abstract fun <A : WorkerAction<IN, OUT>, IN : Any, OUT : Any> launch(
      request: IN,
      provider: WorkerActionProvider<WorkerAction<IN, OUT>, IN, OUT>,
      timeoutTicks: Long = 0,
      delaySeconds: Int = 0,
      root: Boolean = false
   ): DeferredAction<OUT>





   /**
    *
    */
   abstract suspend fun ask(
      request: RoutingContext,
      provider: HttpActionProvider<HttpAction>,
      timeoutTicks: Long = 0
   )

   /**
    *
    */
   abstract suspend fun ask(
      request: RoutingContext,
      provider: HttpActionProvider<HttpAction>,
      timeoutTicks: Long = 0,
      delaySeconds: Int = 0
   )

   /**
    *
    */
   abstract fun rxAsk(
      request: RoutingContext,
      provider: HttpActionProvider<HttpAction>,
      timeoutTicks: Long = 0,
      delaySeconds: Int = 0,
      root: Boolean = false
   ): DeferredAction<Unit>

   /**
    *
    */
   abstract fun launch(
      request: RoutingContext,
      provider: HttpActionProvider<HttpAction>,
      timeoutTicks: Long = 0,
      delaySeconds: Int = 0,
      root: Boolean = false
   ): DeferredAction<Unit>
   

   companion object {
      internal val _default = LocalBroker

      val DEFAULT: ActionBroker
         get() = _default
   }
}

abstract class GatewayBroker {

}

/**
 *
 */
object LocalBroker : ActionBroker() {
   override val id: Int
      get() = 0

   suspend override fun <A : InternalAction<IN, OUT>, IN : Any, OUT : Any> ask(
      request: IN,
      provider: InternalActionProvider<InternalAction<IN, OUT>, IN, OUT>,
      timeoutTicks: Long): OUT {

      var eventLoop = MoveEventLoopGroup.currentEventLoop
      val action = provider.actionProvider.get()

      if (eventLoop == null) {
         eventLoop = provider.eventLoopGroup.next()
         eventLoop.execute {
            action.launch(
               eventLoop,
               provider,
               request,
               timeoutTicks,
               false
            )
         }
         return action.await()
      } else {
         return action.execute0(
            eventLoop,
            provider,
            request,
            timeoutTicks,
            false
         )
      }
   }

   suspend override fun <A : InternalAction<IN, OUT>, IN : Any, OUT : Any> ask(
      request: IN,
      provider: InternalActionProvider<InternalAction<IN, OUT>, IN, OUT>,
      timeoutTicks: Long,
      delaySeconds: Int): OUT {

      var eventLoop = MoveEventLoopGroup.currentEventLoop
      val action = provider.actionProvider.get()

      if (eventLoop == null) {
         eventLoop = provider.eventLoopGroup.next()
         eventLoop.execute {
            action.launch(
               eventLoop,
               provider,
               request,
               timeoutTicks,
               false
            )
         }
         return action.await()
      } else {
         return action.execute0(
            eventLoop,
            provider,
            request,
            timeoutTicks,
            false
         )
      }
   }

   override fun <A : InternalAction<IN, OUT>, IN : Any, OUT : Any> rxAsk(
      request: IN,
      provider: InternalActionProvider<InternalAction<IN, OUT>, IN, OUT>,
      timeoutTicks: Long,
      delaySeconds: Int,
      root: Boolean): DeferredAction<OUT> {

      var eventLoop = MoveEventLoopGroup.currentEventLoop
      val action = provider.actionProvider.get()

      if (eventLoop == null) {
         eventLoop = provider.eventLoopGroup.next()
         eventLoop.execute {
            action.launch(
               eventLoop,
               provider,
               request,
               timeoutTicks,
               false
            )
         }
      } else {
         action.launch(
            eventLoop,
            provider,
            request,
            timeoutTicks,
            false
         )
      }
      return action
   }

   override fun <A : InternalAction<IN, OUT>, IN : Any, OUT : Any> launch(
      request: IN,
      provider: InternalActionProvider<InternalAction<IN, OUT>, IN, OUT>,
      timeoutTicks: Long,
      delaySeconds: Int,
      root: Boolean): DeferredAction<OUT> {

      val eventLoop = provider.eventLoopGroup.next()
      val action = provider.actionProvider.get()

      if (MoveEventLoopGroup.currentEventLoop !== eventLoop) {
         eventLoop.execute {
            action.launch(
               eventLoop,
               provider,
               request,
               0,
               true
            )
         }
      } else {
         action.launch(
            eventLoop,
            provider,
            request,
            0,
            true
         )
      }

      return action
   }







   suspend override fun <A : WorkerAction<IN, OUT>, IN : Any, OUT : Any> ask(
      request: IN,
      provider: WorkerActionProvider<WorkerAction<IN, OUT>, IN, OUT>,
      timeoutTicks: Long): OUT {

      var eventLoop = MoveEventLoopGroup.currentEventLoop
      val action = provider.actionProvider.get()

      if (eventLoop == null) {
         eventLoop = provider.eventLoopGroup.next()
         eventLoop.execute {
            action.launch(
               eventLoop,
               provider,
               request,
               timeoutTicks,
               false
            )
         }
         return action.await()
      } else {
         return action.execute0(
            eventLoop,
            provider,
            request,
            timeoutTicks,
            false
         )
      }
   }

   suspend override fun <A : WorkerAction<IN, OUT>, IN : Any, OUT : Any> ask(
      request: IN,
      provider: WorkerActionProvider<WorkerAction<IN, OUT>, IN, OUT>,
      timeoutTicks: Long,
      delaySeconds: Int): OUT {

      var eventLoop = MoveEventLoopGroup.currentEventLoop
      val action = provider.actionProvider.get()

      if (eventLoop == null) {
         eventLoop = provider.eventLoopGroup.next()
         eventLoop.execute {
            action.launch(
               eventLoop,
               provider,
               request,
               timeoutTicks,
               false
            )
         }
         return action.await()
      } else {
         return action.execute0(
            eventLoop,
            provider,
            request,
            timeoutTicks,
            false
         )
      }
   }

   override fun <A : WorkerAction<IN, OUT>, IN : Any, OUT : Any> rxAsk(
      request: IN,
      provider: WorkerActionProvider<WorkerAction<IN, OUT>, IN, OUT>,
      timeoutTicks: Long,
      delaySeconds: Int,
      root: Boolean): DeferredAction<OUT> {

      var eventLoop = MoveEventLoopGroup.currentEventLoop
      val action = provider.actionProvider.get()

      if (eventLoop == null) {
         eventLoop = provider.eventLoopGroup.next()
         eventLoop.execute {
            action.launch(
               eventLoop,
               provider,
               request,
               timeoutTicks,
               false
            )
         }
      } else {
         action.launch(
            eventLoop,
            provider,
            request,
            timeoutTicks,
            false
         )
      }
      return action
   }

   override fun <A : WorkerAction<IN, OUT>, IN : Any, OUT : Any> launch(
      request: IN,
      provider: WorkerActionProvider<WorkerAction<IN, OUT>, IN, OUT>,
      timeoutTicks: Long,
      delaySeconds: Int,
      root: Boolean): DeferredAction<OUT> {

      val eventLoop = provider.eventLoopGroup.next()
      val action = provider.actionProvider.get()

      if (MoveEventLoopGroup.currentEventLoop !== eventLoop) {
         eventLoop.execute {
            action.launch(
               eventLoop,
               provider,
               request,
               0,
               true
            )
         }
      } else {
         action.launch(
            eventLoop,
            provider,
            request,
            0,
            true
         )
      }

      return action
   }


   suspend override fun ask(request: RoutingContext, provider: HttpActionProvider<HttpAction>, timeoutTicks: Long) {
      var eventLoop = MoveEventLoopGroup.currentEventLoop
      val action = provider.actionProvider.get()

      if (eventLoop == null) {
         eventLoop = provider.eventLoopGroup.next()
         eventLoop.execute {
            action.launch(
               eventLoop,
               provider,
               request,
               timeoutTicks,
               false
            )
         }
         return action.await()
      } else {
         return action.execute0(
            eventLoop,
            provider,
            request,
            timeoutTicks,
            false
         )
      }
   }

   suspend override fun ask(request: RoutingContext, provider: HttpActionProvider<HttpAction>, timeoutTicks: Long, delaySeconds: Int) {
      var eventLoop = MoveEventLoopGroup.currentEventLoop
      val action = provider.actionProvider.get()

      if (eventLoop == null) {
         eventLoop = provider.eventLoopGroup.next()
         eventLoop.execute {
            action.launch(
               eventLoop,
               provider,
               request,
               timeoutTicks,
               false
            )
         }
         return action.await()
      } else {
         return action.execute0(
            eventLoop,
            provider,
            request,
            timeoutTicks,
            false
         )
      }
   }

   override fun rxAsk(request: RoutingContext, provider: HttpActionProvider<HttpAction>, timeoutTicks: Long, delaySeconds: Int, root: Boolean): DeferredAction<Unit> {
      val eventLoop = provider.eventLoopGroup.next()
      val action = provider.actionProvider.get()

      if (MoveEventLoopGroup.currentEventLoop !== eventLoop) {
         eventLoop.execute {
            action.launch(
               eventLoop,
               provider,
               request,
               0,
               true
            )
         }
      } else {
         action.launch(
            eventLoop,
            provider,
            request,
            0,
            true
         )
      }

      return action
   }

   override fun launch(request: RoutingContext, provider: HttpActionProvider<HttpAction>, timeoutTicks: Long, delaySeconds: Int, root: Boolean): DeferredAction<Unit> {
      val eventLoop = provider.eventLoopGroup.next()
      val action = provider.actionProvider.get()

      if (MoveEventLoopGroup.currentEventLoop !== eventLoop) {
         eventLoop.execute {
            action.launch(
               eventLoop,
               provider,
               request,
               0,
               true
            )
         }
      } else {
         action.launch(
            eventLoop,
            provider,
            request,
            0,
            true
         )
      }

      return action
   }
}

///**
// *
// */
//abstract class BrokerReceipt<T>(val rx: Single<T>, val id: String = NUID.nextGlobal()) {
//   abstract val nodeId: String
//   abstract val brokerId: String
//}
//
//class SQSBrokerReceipt<T>(rx: Single<T>, id: String = NUID.nextGlobal()) : BrokerReceipt<T>(rx, id) {
//   var md5OfBody: String? = null
//   var _brokerId: String? = null
//
//   override val nodeId: String
//      get() = ""
//   override val brokerId: String
//      get() = _brokerId ?: ""
//}