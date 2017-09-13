package move.action

import io.vertx.ext.web.RoutingContext
import rx.Single
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import javax.inject.Inject

/**
 * Action Producers are in charge of dispatching an action request through
 * an Action Broker.
 */
abstract class ActionProducer<A : Action<IN, OUT>, IN : Any, OUT : Any, P : ActionProvider<A, IN, OUT>> {
   private val entry: ProducerEntry<A, IN, OUT, P>

   lateinit var provider: P
      get
      private set

   constructor() {
      @Suppress("LeakingThis")
      entry = register(actionClass, this)
   }

   abstract val actionClass: Class<A>

   @Inject
   internal fun injectProvider(provider: P) {
      this.provider = provider
      entry.injectProvider(provider)
   }

   private class ProducerEntry
   <A : Action<IN, OUT>,
      IN : Any,
      OUT : Any,
      P
      : ActionProvider<A, IN, OUT>>(first: ActionProducer<A, IN, OUT, P>) {

      val list = mutableListOf<ActionProducer<A, IN, OUT, P>>(first)
      var provider: P? = null

      @Synchronized
      internal fun injectProvider(provider: P) {
         this.provider = provider
         list.forEach { it.provider = provider }
         list.clear()
      }
   }

   companion object {
      private val registry: MutableMap<Class<*>, ProducerEntry<*, *, *, *>> = mutableMapOf()

      @Synchronized
      private fun
         <PRODUCER : ActionProducer<A, IN, OUT, P>,
            A : Action<IN, OUT>,
            IN : Any,
            OUT : Any,
            P : ActionProvider<A, IN, OUT>> register(actionClass: Class<A>,
                                                     producer: PRODUCER): ProducerEntry<A, IN, OUT, P> {
         if (registry.containsKey(actionClass)) {
            @Suppress("UNCHECKED_CAST")
            val entry = registry[actionClass] as ProducerEntry<A, IN, OUT, P>

            if (entry.provider != null)
               producer.provider = entry.provider!!

            return entry
         }

         val entry = ProducerEntry(producer)
         registry[actionClass] = entry
         return entry
      }
   }
}

/**
 *
 */
abstract class InternalActionProducer
<A : JobAction<IN, OUT>, IN : Any, OUT : Any, P : InternalActionProvider<A, IN, OUT>>
   : ActionProducer<A, IN, OUT, P>() {
   val provider0: InternalActionProvider<JobAction<IN, OUT>, IN, OUT> get() = provider.self

   /**
    * Waits for response.
    * This executes as a child to the current action.
    *
    * Exception is thrown if it times out.
    */
   suspend open infix fun ask(request: IN): OUT {
      return provider.broker.ask(
         request = request,
         provider = provider0,
         timeoutTicks = provider0.timeoutTicks
      )
   }

   suspend open fun ask(request: IN,
                        timeout: Long = 0,
                        unit: TimeUnit = TimeUnit.MILLISECONDS): OUT {
      return provider.broker.ask(
         request = request,
         provider = provider0,
         timeoutTicks = MoveEventLoop.calculateTicks(timeout, unit)
      )
   }

   infix open fun rxAsk(request: IN): DeferredAction<OUT> {
      return provider.broker.rxAsk(
         request = request,
         provider = provider0,
         timeoutTicks = provider0.timeoutTicks
      )
   }

   @Deprecated("", ReplaceWith("rxAsk"))
   open fun single(request: IN): Single<OUT> {
      return rxAsk(request).asSingle()
   }

   infix open fun launch(request: IN): DeferredAction<OUT> {
      return provider.broker.launch(
         request = request,
         provider = provider0,
         timeoutTicks = provider0.timeoutTicks
      )
   }

   open fun launch(request: IN,
                   timeout: Long,
                   unit: TimeUnit = TimeUnit.MILLISECONDS): DeferredAction<OUT> {
      return provider.broker.launch(
         request = request,
         provider = provider0,
         timeoutTicks = MoveEventLoop.calculateTicks(timeout, unit)
      )
   }
}

abstract class InternalActionProducerWithBuilder
<A : JobAction<IN, OUT>, IN : Any, OUT : Any, P : InternalActionProvider<A, IN, OUT>>
   : InternalActionProducer<A, IN, OUT, P>() {
   abstract fun createRequest(): IN

   infix suspend open fun ask(block: IN.() -> Unit): OUT {
      return ask(createRequest().apply(block))
   }

   infix open fun rxAsk(block: IN.() -> Unit): DeferredAction<OUT> {
      return rxAsk(createRequest().apply(block))
   }

   infix open fun launch(block: IN.() -> Unit): DeferredAction<OUT> {
      return launch(createRequest().apply(block))
   }

   @Deprecated("", ReplaceWith("rxAsk"))
   open fun singleBuilder(consumer: Consumer<IN>): Single<OUT> {
      val request = createRequest()
      consumer.accept(request)
      return single(request)
   }
}


abstract class WorkerActionProducer
<A : JobAction<IN, OUT>, IN : Any, OUT : Any, P : WorkerActionProvider<A, IN, OUT>>
   : ActionProducer<A, IN, OUT, P>() {
   val provider0: WorkerActionProvider<JobAction<IN, OUT>, IN, OUT> get() = provider0.self

   /**
    * Waits for response.
    * This executes as a child to the current action.
    *
    * Exception is thrown if it times out.
    */
   suspend open infix fun ask(request: IN): OUT {
      return provider.broker.ask(
         request = request,
         provider = provider0,
         timeoutTicks = provider0.timeoutTicks
      )
   }

   suspend open fun ask(request: IN,
                        timeout: Long = 0,
                        unit: TimeUnit = TimeUnit.MILLISECONDS): OUT {
      return provider.broker.ask(
         request = request,
         provider = provider0,
         timeoutTicks = MoveEventLoop.calculateTicks(timeout, unit)
      )
   }

   infix fun rxAsk(request: IN): DeferredAction<OUT> {
      return provider.broker.rxAsk(
         request = request,
         provider = provider0,
         timeoutTicks = provider0.timeoutTicks
      )
   }

   @Deprecated("", ReplaceWith("rxAsk"))
   open fun single(request: IN): Single<OUT> {
      return rxAsk(request).asSingle()
   }

   infix fun launch(request: IN): DeferredAction<OUT> {
      return provider.broker.launch(
         request = request,
         provider = provider0,
         timeoutTicks = provider0.timeoutTicks
      )
   }

   fun launch(request: IN,
              timeout: Long,
              unit: TimeUnit = TimeUnit.MILLISECONDS): DeferredAction<OUT> {
      return provider.broker.launch(
         request = request,
         provider = provider0,
         timeoutTicks = MoveEventLoop.calculateTicks(timeout, unit)
      )
   }

//   /**
//    * Fire and Forget
//    */
//   infix fun send(request: IN) {
//      send(request, 0)
//   }

//   /**
//    * Ask with an optional user specified timeout.
//    */
//   fun send(request: IN, timeout: Long = 0, unit: TimeUnit = TimeUnit.MILLISECONDS) {
//      provider.broker.rxAsk(
//         request = request,
//         provider = provider,
//         timeout = timeout,
//         unit = unit
//      ).subscribe()
//   }


//   /**
//    *
//    */
//   infix fun rxAsk(request: IN): Single<OUT> {
//      return provider.broker.rxAsk(
//         request = request,
//         provider = provider
//      )
//   }

//   /**
//    *
//    */
//   fun rxAsk(request: IN, timeout: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): Single<OUT> {
//      return provider.broker.rxAsk(
//         request = request,
//         provider = provider,
//         timeout = timeout,
//         unit = unit
//      )
//   }

//   /**
//    * "tell" pattern for long running and/or Durable Queue broker.
//    */
//   suspend infix fun tell(request: IN): BrokerReceipt<OUT> {
//      return LocalBroker.Receipt(provider.create().rx(request))
//   }
//
//   /**
//    *
//    */
//   infix fun rxTell(request: IN): Single<BrokerReceipt<OUT>> {
//      return Single.just(LocalBroker.Receipt(provider.create().rx(request)))
//   }
//
//   /**
//    *
//    */
//   suspend fun tell(request: IN,
//                    delaySeconds: Int = 0,
//                    broker: ActionBroker = ActionBroker.DEFAULT): BrokerReceipt<OUT> {
//      return LocalBroker.Receipt(provider.create().rx(request))
//   }
//
//   /**
//    *
//    */
//   suspend fun rxTell(request: IN,
//                      delaySeconds: Int = 0,
//                      broker: ActionBroker = ActionBroker.DEFAULT): Single<BrokerReceipt<OUT>> {
//      return Single.just(LocalBroker.Receipt(provider.create().rx(request)))
//   }
}

abstract class WorkerActionProducerWithBuilder
<A : JobAction<IN, OUT>, IN : Any, OUT : Any, P : WorkerActionProvider<A, IN, OUT>>
   : WorkerActionProducer<A, IN, OUT, P>() {
   abstract fun createRequest(): IN

   suspend infix open fun ask(block: IN.() -> Unit): OUT {
      return ask(createRequest().apply(block))
   }

   infix open fun rxAsk(block: IN.() -> Unit): DeferredAction<OUT> {
      return rxAsk(createRequest().apply(block))
   }

   infix open fun launch(block: IN.() -> Unit): DeferredAction<OUT> {
      return launch(createRequest().apply(block))
   }

   @Deprecated("", ReplaceWith("rxAsk"))
   open fun singleBuilder(consumer: Consumer<IN>): Single<OUT> {
      val request = createRequest()
      consumer.accept(request)
      return single(request)
   }
}


abstract class HttpActionProducer
<A : HttpAction, P : HttpActionProvider<A>>
   : ActionProducer<A, RoutingContext, Unit, P>() {

   fun visibleTo(visibility: ActionVisibility) = provider.visibility == visibility

   val provider0: HttpActionProvider<HttpAction> get() = provider.self

   /**
    * Waits for response.
    * This executes as a child to the current action.
    *
    * Exception is thrown if it times out.
    */
   suspend open infix fun ask(request: RoutingContext) {
      provider.broker.ask(
         request = request,
         provider = provider0,
         timeoutTicks = provider0.timeoutTicks
      )
   }

   suspend open fun ask(request: RoutingContext,
                        timeout: Long = 0,
                        unit: TimeUnit = TimeUnit.MILLISECONDS) {
      return provider.broker.ask(
         request = request,
         provider = provider0,
         timeoutTicks = MoveEventLoop.calculateTicks(timeout, unit)
      )
   }

   infix fun rxAsk(request: RoutingContext): DeferredAction<Unit> {
      return provider.broker.rxAsk(
         request = request,
         provider = provider0,
         timeoutTicks = provider0.timeoutTicks
      )
   }

   @Deprecated("", ReplaceWith("rxAsk"))
   fun single(request: RoutingContext): Single<Unit> {
      return rxAsk(request).asSingle()
   }

   infix fun launch(request: RoutingContext): DeferredAction<Unit> {
      return provider.broker.launch(
         request = request,
         provider = provider0,
         timeoutTicks = provider0.timeoutTicks
      )
   }

   fun launch(request: RoutingContext,
              timeout: Long,
              unit: TimeUnit = TimeUnit.MILLISECONDS): DeferredAction<Unit> {
      return provider.broker.launch(
         request = request,
         provider = provider0,
         timeoutTicks = MoveEventLoop.calculateTicks(timeout, unit)
      )
   }
}