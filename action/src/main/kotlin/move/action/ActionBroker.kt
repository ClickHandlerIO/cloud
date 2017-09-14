package move.action

import io.vertx.ext.web.RoutingContext
import io.vertx.rxjava.core.Vertx

/**
 *
 */
val LOCAL_BROKER = LocalBroker

var _GATEWAY_BROKER: ActionBroker = LOCAL_BROKER

fun changeGatewayBroker(broker: ActionBroker) {
   _GATEWAY_BROKER = broker

   Actions.worker.forEach {
      it.provider.broker = broker
   }
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
 * In charge of delivering to an ActionKernel whether Local or Remote.
 */
abstract class ActionBroker {
   abstract val id: Int

   /**
    *
    */
   abstract suspend fun <A : JobAction<IN, OUT>, IN : Any, OUT : Any> ask(
      request: IN,
      provider: InternalActionProvider<A, IN, OUT>,
      timeoutTicks: Long = 0
   ): OUT

   /**
    *
    */
   abstract suspend fun <A : JobAction<IN, OUT>, IN : Any, OUT : Any> ask(
      request: IN,
      provider: InternalActionProvider<A, IN, OUT>,
      timeoutTicks: Long = 0,
      delaySeconds: Int = 0
   ): OUT

   /**
    *
    */
   abstract fun <A : JobAction<IN, OUT>, IN : Any, OUT : Any> rxAsk(
      request: IN,
      provider: InternalActionProvider<A, IN, OUT>,
      timeoutTicks: Long = 0,
      delaySeconds: Int = 0,
      root: Boolean = false
   ): DeferredAction<OUT>


   /**
    *
    */
   abstract fun <A : JobAction<IN, OUT>, IN : Any, OUT : Any> launch(
      request: IN,
      provider: InternalActionProvider<A, IN, OUT>,
      timeoutTicks: Long = 0,
      delaySeconds: Int = 0,
      root: Boolean = false
   ): DeferredAction<OUT>

   /**
    *
    */
   abstract fun <A : JobAction<IN, OUT>, IN : Any, OUT : Any> launch(
      request: IN,
      provider: InternalActionProvider<A, IN, OUT>,
      token: ActionToken = NoToken,
      timeoutTicks: Long = 0,
      delaySeconds: Int = 0,
      root: Boolean = false
   ): DeferredAction<OUT>


   /**
    *
    */
   abstract suspend fun <A : JobAction<IN, OUT>, IN : Any, OUT : Any> ask(
      request: IN,
      provider: WorkerActionProvider<A, IN, OUT>,
      timeoutTicks: Long = 0
   ): OUT

   /**
    *
    */
   abstract suspend fun <A : JobAction<IN, OUT>, IN : Any, OUT : Any> ask(
      request: IN,
      provider: WorkerActionProvider<A, IN, OUT>,
      timeoutTicks: Long = 0,
      delaySeconds: Int = 0
   ): OUT

   /**
    *
    */
   abstract fun <A : JobAction<IN, OUT>, IN : Any, OUT : Any> rxAsk(
      request: IN,
      provider: WorkerActionProvider<A, IN, OUT>,
      timeoutTicks: Long = 0,
      delaySeconds: Int = 0,
      root: Boolean = false
   ): DeferredAction<OUT>

   /**
    *
    */
   abstract fun <A : JobAction<IN, OUT>, IN : Any, OUT : Any> launch(
      request: IN,
      provider: WorkerActionProvider<A, IN, OUT>,
      timeoutTicks: Long = 0,
      delaySeconds: Int = 0,
      root: Boolean = false
   ): DeferredAction<OUT>

   /**
    *
    */
   abstract fun <A : JobAction<IN, OUT>, IN : Any, OUT : Any> launch(
      request: IN,
      provider: WorkerActionProvider<A, IN, OUT>,
      token: ActionToken = NoToken,
      timeoutTicks: Long = 0,
      delaySeconds: Int = 0,
      root: Boolean = false
   ): DeferredAction<OUT>


   /**
    *
    */
   abstract suspend fun <A : HttpAction> ask(
      request: RoutingContext,
      provider: HttpActionProvider<A>,
      timeoutTicks: Long = 0
   )

   /**
    *
    */
   abstract suspend fun <A : HttpAction> ask(
      request: RoutingContext,
      provider: HttpActionProvider<A>,
      timeoutTicks: Long = 0,
      delaySeconds: Int = 0
   )

   /**
    *
    */
   abstract fun <A : HttpAction> rxAsk(
      request: RoutingContext,
      provider: HttpActionProvider<A>,
      timeoutTicks: Long = 0,
      delaySeconds: Int = 0,
      root: Boolean = false
   ): DeferredAction<Unit>

   /**
    *
    */
   abstract fun <A : HttpAction> launch(
      request: RoutingContext,
      provider: HttpActionProvider<A>,
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

/**
 * 3rd party messaging systems/queues/brokers can become a Gateway Broker.
 * A Gateway Broker assumes the responsibility of delivering a message to
 * a Worker node which will then go to the LocalBroker or the NATS broker.
 * A GatewayBroker only supports brokering for Workers or Actors.
 */
abstract class GatewayBroker : ActionBroker() {
   suspend override fun <A : JobAction<IN, OUT>, IN : Any, OUT : Any> ask(
      request: IN,
      provider: InternalActionProvider<A, IN, OUT>,
      timeoutTicks: Long): OUT {

      return LocalBroker.ask(request, provider, timeoutTicks)
   }

   suspend override fun <A : JobAction<IN, OUT>, IN : Any, OUT : Any> ask(
      request: IN,
      provider: InternalActionProvider<A, IN, OUT>,
      timeoutTicks: Long,
      delaySeconds: Int): OUT {

      return LocalBroker.ask(request, provider, timeoutTicks, delaySeconds)
   }

   override fun <A : JobAction<IN, OUT>, IN : Any, OUT : Any> rxAsk(
      request: IN,
      provider: InternalActionProvider<A, IN, OUT>,
      timeoutTicks: Long,
      delaySeconds: Int,
      root: Boolean): DeferredAction<OUT> {

      return LocalBroker.rxAsk(request, provider, timeoutTicks, delaySeconds, root)
   }

   override fun <A : JobAction<IN, OUT>, IN : Any, OUT : Any> launch(
      request: IN,
      provider: InternalActionProvider<A, IN, OUT>,
      timeoutTicks: Long,
      delaySeconds: Int,
      root: Boolean): DeferredAction<OUT> {

      return LocalBroker.launch(request, provider, timeoutTicks, delaySeconds, root)
   }

   suspend override fun <A : HttpAction> ask(request: RoutingContext,
                            provider: HttpActionProvider<A>,
                            timeoutTicks: Long) {
      LocalBroker.ask(request, provider, timeoutTicks)
   }

   suspend override fun <A : HttpAction> ask(request: RoutingContext,
                            provider: HttpActionProvider<A>,
                            timeoutTicks: Long,
                            delaySeconds: Int) {
      LocalBroker.ask(request, provider, timeoutTicks, delaySeconds)
   }

   override fun <A : HttpAction> rxAsk(request: RoutingContext,
                      provider: HttpActionProvider<A>,
                      timeoutTicks: Long,
                      delaySeconds: Int,
                      root: Boolean): DeferredAction<Unit> {
      return LocalBroker.rxAsk(request, provider, timeoutTicks, delaySeconds, root)
   }

   override fun <A : HttpAction> launch(request: RoutingContext,
                       provider: HttpActionProvider<A>,
                       timeoutTicks: Long,
                       delaySeconds: Int,
                       root: Boolean): DeferredAction<Unit> {
      return LocalBroker.launch(request, provider, timeoutTicks, delaySeconds, root)
   }
}

/**
 *
 */
object LocalBroker : ActionBroker() {
   override val id: Int
      get() = 0

   suspend override fun <A : JobAction<IN, OUT>, IN : Any, OUT : Any> ask(
      request: IN,
      provider: InternalActionProvider<A, IN, OUT>,
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
               NoToken,
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

   suspend override fun <A : JobAction<IN, OUT>, IN : Any, OUT : Any> ask(
      request: IN,
      provider: InternalActionProvider<A, IN, OUT>,
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
               NoToken,
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

   override fun <A : JobAction<IN, OUT>, IN : Any, OUT : Any> rxAsk(
      request: IN,
      provider: InternalActionProvider<A, IN, OUT>,
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
               NoToken,
               timeoutTicks,
               false
            )
         }
      } else {
         action.launch(
            eventLoop,
            provider,
            request,
            NoToken,
            timeoutTicks,
            false
         )
      }
      return action
   }

   override fun <A : JobAction<IN, OUT>, IN : Any, OUT : Any> launch(
      request: IN,
      provider: InternalActionProvider<A, IN, OUT>,
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
               NoToken,
               timeoutTicks,
               true
            )
         }
      } else {
         action.launch(
            eventLoop,
            provider,
            request,
            NoToken,
            timeoutTicks,
            true
         )
      }

      return action
   }

   override fun <A : JobAction<IN, OUT>, IN : Any, OUT : Any> launch(
      request: IN,
      provider: InternalActionProvider<A, IN, OUT>,
      token: ActionToken,
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
               token,
               timeoutTicks,
               true
            )
         }
      } else {
         action.launch(
            eventLoop,
            provider,
            request,
            token,
            timeoutTicks,
            true
         )
      }

      return action
   }

   suspend override fun <A : JobAction<IN, OUT>, IN : Any, OUT : Any> ask(
      request: IN,
      provider: WorkerActionProvider<A, IN, OUT>,
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
               NoToken,
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

   suspend override fun <A : JobAction<IN, OUT>, IN : Any, OUT : Any> ask(
      request: IN,
      provider: WorkerActionProvider<A, IN, OUT>,
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
               NoToken,
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

   override fun <A : JobAction<IN, OUT>, IN : Any, OUT : Any> rxAsk(
      request: IN,
      provider: WorkerActionProvider<A, IN, OUT>,
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
               NoToken,
               timeoutTicks,
               false
            )
         }
      } else {
         action.launch(
            eventLoop,
            provider,
            request,
            NoToken,
            timeoutTicks,
            false
         )
      }
      return action
   }

   override fun <A : JobAction<IN, OUT>, IN : Any, OUT : Any> launch(
      request: IN,
      provider: WorkerActionProvider<A, IN, OUT>,
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
               NoToken,
               timeoutTicks,
               true
            )
         }
      } else {
         action.launch(
            eventLoop,
            provider,
            request,
            NoToken,
            timeoutTicks,
            true
         )
      }

      return action
   }

   override fun <A : JobAction<IN, OUT>, IN : Any, OUT : Any> launch(
      request: IN,
      provider: WorkerActionProvider<A, IN, OUT>,
      token: ActionToken,
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
               token,
               timeoutTicks,
               true
            )
         }
      } else {
         action.launch(
            eventLoop,
            provider,
            request,
            token,
            timeoutTicks,
            true
         )
      }

      return action
   }

   suspend override fun <A : HttpAction> ask(request: RoutingContext,
                                             provider: HttpActionProvider<A>,
                                             timeoutTicks: Long) {

      var eventLoop = MoveEventLoopGroup.currentEventLoop
      val action = provider.actionProvider.get()

      if (eventLoop == null) {
         eventLoop = provider.eventLoopGroup.next()
         eventLoop.execute {
            action.launch(
               eventLoop,
               provider,
               request,
               NoToken,
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

   suspend override fun <A : HttpAction> ask(request: RoutingContext,
                            provider: HttpActionProvider<A>,
                            timeoutTicks: Long,
                            delaySeconds: Int) {

      var eventLoop = MoveEventLoopGroup.currentEventLoop
      val action = provider.actionProvider.get()

      if (eventLoop == null) {
         eventLoop = provider.eventLoopGroup.next()
         eventLoop.execute {
            action.launch(
               eventLoop,
               provider,
               request,
               NoToken,
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

   override fun <A : HttpAction> rxAsk(request: RoutingContext,
                      provider: HttpActionProvider<A>,
                      timeoutTicks: Long,
                      delaySeconds: Int,
                      root: Boolean): DeferredAction<Unit> {

      var eventLoop = MoveEventLoopGroup.currentEventLoop
      val action = provider.actionProvider.get()

      if (eventLoop == null) {
         eventLoop = provider.eventLoopGroup.next()
         eventLoop.execute {
            action.launch(
               eventLoop,
               provider,
               request,
               NoToken,
               timeoutTicks,
               true
            )
         }
      } else {
         action.launch(
            eventLoop,
            provider,
            request,
            NoToken,
            timeoutTicks,
            true
         )
      }

      return action
   }

   override fun <A : HttpAction> launch(request: RoutingContext,
                       provider: HttpActionProvider<A>,
                       timeoutTicks: Long,
                       delaySeconds: Int,
                       root: Boolean): DeferredAction<Unit> {

      var eventLoop = MoveEventLoopGroup.currentEventLoop
      val action = provider.actionProvider.get()

      if (eventLoop == null) {
         eventLoop = provider.eventLoopGroup.next()
         eventLoop.execute {
            action.launch(
               eventLoop,
               provider,
               request,
               NoToken,
               timeoutTicks,
               true
            )
         }
      } else {
         action.launch(
            eventLoop,
            provider,
            request,
            NoToken,
            timeoutTicks,
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