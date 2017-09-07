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
         P : ActionProvider<A, IN, OUT>> register(cls: Class<PRODUCER>, producer: PRODUCER): ProducerEntry<A, IN, OUT, P> {
         val rawClass = TypeResolver
            .resolveRawClass(
               ActionProducer::class.java,
               cls
            )

         val actionClass = TypeResolver.resolveRawArgument(rawClass.typeParameters[0], cls)

         if (registry.containsKey(actionClass)) {
            @Suppress("UNCHECKED_CAST")
            val entry = registry[actionClass] as ProducerEntry<A, IN, OUT, P>

            if (entry.provider != null)
               producer.provider = entry.provider!!

            return entry
         }

//         val t = TypeResolver.resolveGenericType(cls, ActionProducer::class.java)
//         TypeResolver.resolveRawArgument()
//
//         var c: Class<*>? = cls
//
//         while (c != null && c != Any::class.java) {
//
//
//            c = c.superclass
//         }

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
//      provider.defaultBroker.rxAsk(
//         request = request,
//         provider = provider,
//         timeout = timeout,
//         unit = unit
//      ).subscribe()
//   }

   /**
    * Waits for response.
    *
    * Exception is thrown if it times out.
    */
   suspend infix fun ask(request: IN): OUT {
      return provider.defaultBroker.ask(
         request = request,
         provider = provider
      )
   }

   /**
    * Ask with an optional user specified timeout.
    */
   suspend fun ask(request: IN, timeout: Long = 0, unit: TimeUnit = TimeUnit.MILLISECONDS): OUT {
      return provider.defaultBroker.ask(
         request = request,
         provider = provider,
         timeout = timeout,
         unit = unit
      )
   }

//   /**
//    *
//    */
//   infix fun rxAsk(request: IN): Single<OUT> {
//      return provider.defaultBroker.rxAsk(
//         request = request,
//         provider = provider
//      )
//   }

//   /**
//    *
//    */
//   fun rxAsk(request: IN, timeout: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): Single<OUT> {
//      return provider.defaultBroker.rxAsk(
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
//      provider.defaultBroker.rxAsk(
//         request = request,
//         provider = provider,
//         timeout = timeout,
//         unit = unit
//      ).subscribe()
//   }

   /**
    * Waits for response.
    *
    * Exception is thrown if it times out.
    */
   suspend open infix fun ask(request: IN): OUT {
      return provider.defaultBroker.ask(
         request = request,
         provider = provider
      )
   }

   /**
    * Ask with an optional user specified timeout.
    */
   suspend open fun ask(request: IN, timeout: Long = 0, unit: TimeUnit = TimeUnit.MILLISECONDS): OUT {
      return provider.defaultBroker.ask(
         request = request,
         provider = provider,
         timeout = timeout,
         unit = unit
      )
   }

   infix fun rxAsk(request: IN): Deferred<OUT> {
      return provider.defaultBroker.rxAsk(
         request = request,
         provider = provider
      )
   }

//   /**
//    *
//    */
//   infix fun rxAsk(request: IN): Single<OUT> {
//      return provider.defaultBroker.rxAsk(
//         request = request,
//         provider = provider
//      )
//   }
//
//   /**
//    *
//    */
//   fun rxAsk(request: IN, timeout: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): Single<OUT> {
//      return provider.defaultBroker.rxAsk(
//         request = request,
//         provider = provider,
//         timeout = timeout,
//         unit = unit
//      )
//   }
}

open class HttpActionProducer<A : HttpAction, P : HttpActionProvider<A>> @Inject constructor() : ActionProducer<A, RoutingContext, Unit, P>() {
   fun visibleTo(visibility: ActionVisibility) = provider.visibility == visibility

   /**
    * Waits for response.
    *
    * Exception is thrown if it times out.
    */
   suspend open infix fun ask(request: RoutingContext) {
      return provider.defaultBroker.ask(
         request = request,
         provider = provider
      )
   }

   /**
    * Ask with an optional user specified timeout.
    */
   suspend open fun ask(request: RoutingContext, timeout: Long = 0, unit: TimeUnit = TimeUnit.MILLISECONDS) {
      return provider.defaultBroker.ask(
         request = request,
         provider = provider,
         timeout = timeout,
         unit = unit
      )
   }

   infix fun rxAsk(request: RoutingContext): JobAction<RoutingContext, Unit> {
      return provider.defaultBroker.rxAsk(
         request = request,
         provider = provider
      )
   }
}