package move.action

import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.experimental.Deferred
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 *
 */
open class ActionProducer<A : Action<IN, OUT>, IN : Any, OUT : Any, P : ActionProvider<A, IN, OUT>> {
   private val entry: ProducerEntry<A, IN, OUT, P>

   internal lateinit var provider: P
      get

   @Inject constructor() {
      entry = register(javaClass, this)
   }

   @Inject
   internal fun injectProvider(provider: P) {
      this.provider = provider
      entry.injectProvider(provider)
   }

   private class ProducerEntry<A : Action<IN, OUT>, IN : Any, OUT : Any, P : ActionProvider<A, IN, OUT>>(first: ActionProducer<A, IN, OUT, P>) {
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
      private fun <
         PRODUCER : ActionProducer<A, IN, OUT, P>,
         A : Action<IN, OUT>,
         IN : Any,
         OUT : Any,
         P : ActionProvider<A, IN, OUT>> register(producerClass: Class<PRODUCER>, producer: PRODUCER): ProducerEntry<A, IN, OUT, P> {
         val rawProducerClass = TypeResolver
            .resolveRawClass(
               ActionProducer::class.java,
               producerClass
            )

         val actionClass = TypeResolver
            .resolveRawArgument(
               rawProducerClass.typeParameters[0],
               producerClass
            )

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

open class WorkerActionProducer<A : WorkerAction<IN, OUT>, IN : Any, OUT : Any, P : WorkerActionProvider<A, IN, OUT>> @Inject constructor() : ActionProducer<A, IN, OUT, P>() {
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


/**
 *
 */
open class InternalActionProducer<A : InternalAction<IN, OUT>, IN : Any, OUT : Any, P : InternalActionProvider<A, IN, OUT>> @Inject constructor() : ActionProducer<A, IN, OUT, P>() {
   val provider0: InternalActionProvider<InternalAction<IN, OUT>, IN, OUT> get() = provider as InternalActionProvider<InternalAction<IN, OUT>, IN, OUT>

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

   suspend open fun ask(request: IN, timeout: Long = 0, unit: TimeUnit = TimeUnit.MILLISECONDS): OUT {
      return provider.broker.ask(
         request = request,
         provider = provider0,
         timeoutTicks = MoveEventLoop.countTicks(timeout, unit)
      )
   }

   infix fun rxAsk(request: IN): Deferred<OUT> {
      return provider.broker.rxAsk(
         request = request,
         provider = provider0,
         timeoutTicks = provider0.timeoutTicks
      )
   }

   infix fun launch(request: IN): Deferred<OUT> {
      return provider.broker.launch(
         request = request,
         provider = provider0,
         timeoutTicks = provider0.timeoutTicks
      )
   }

   fun launch(request: IN, timeout: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): Deferred<OUT> {
      return provider.broker.launch(
         request = request,
         provider = provider0,
         timeoutTicks = MoveEventLoop.countTicks(timeout, unit)
      )
   }
}

open class HttpActionProducer<A : HttpAction, P : HttpActionProvider<A>> @Inject constructor() : ActionProducer<A, RoutingContext, Unit, P>() {
   fun visibleTo(visibility: ActionVisibility) = provider.visibility == visibility


}