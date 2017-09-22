package move.action

import io.netty.buffer.ByteBuf
import io.vertx.ext.web.RoutingContext
import move.NUID
import move.Wire
import rx.Single
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import javax.inject.Inject
import javax.inject.Provider

/**
 * Action Producers are in charge of dispatching an action request through
 * an Action Broker.
 */
abstract class ActionProducer
<A : Action<IN, OUT>,
   IN : Any,
   OUT : Any,
   P : ActionProvider<A, IN, OUT>> {

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
      entry.injectProvider(provider)
      onProviderSet(provider)
   }

   protected open fun onProviderSet(provider: P) {
      this.provider = provider
   }

   /**
    *
    */
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
         list.forEach { it.onProviderSet(provider) }
         list.clear()
      }
   }

   companion object {
      private val registry = mutableMapOf<Class<*>, ProducerEntry<*, *, *, *>>()

      /**
       *
       */
      @Synchronized
      private fun
         <PRODUCER : ActionProducer<A, IN, OUT, P>,
            A : Action<IN, OUT>,
            IN : Any,
            OUT : Any,
            P : ActionProvider<A, IN, OUT>> register(
         actionClass: Class<A>,
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
<A : JobAction<IN, OUT>,
   IN : Any,
   OUT : Any,
   P : InternalActionProvider<A, IN, OUT>>
   : ActionProducer<A, IN, OUT, P>() {

   /**
    * Waits for response.
    * This executes as a child to the current action.
    *
    * Exception is thrown if it times out.
    */
   suspend open infix fun ask(request: IN): OUT {
      return provider.broker.ask(
         request = request,
         provider = provider,
         timeoutTicks = provider.timeoutTicks
      )
   }

   suspend open infix fun await(request: IN): OUT {
      return provider.broker.ask(
         request = request,
         provider = provider,
         timeoutTicks = provider.timeoutTicks
      )
   }

   @Deprecated("!!! THIS IS BLOCKING !!!")
   open infix fun execute(request: IN): OUT {
      return provider.broker.rxAsk(
         request = request,
         provider = provider,
         timeoutTicks = provider.timeoutTicks
      ).asSingle().toBlocking().value()
   }

   @Deprecated("!!! THIS IS BLOCKING !!!")
   open infix fun blocking(request: IN): OUT {
      return provider.broker.rxAsk(
         request = request,
         provider = provider,
         timeoutTicks = provider.timeoutTicks
      ).asSingle().toBlocking().value()
   }

   suspend open fun ask(request: IN,
                        timeout: Long = 0,
                        unit: TimeUnit = TimeUnit.MILLISECONDS): OUT {
      return provider.broker.ask(
         request = request,
         provider = provider,
         timeoutTicks = MEventLoop.calculateTicks(timeout, unit)
      )
   }

   infix open fun rxAsk(request: IN): DeferredAction<OUT> {
      return provider.broker.rxAsk(
         request = request,
         provider = provider,
         timeoutTicks = provider.timeoutTicks
      )
   }

   @Deprecated("", ReplaceWith("rxAsk"))
   open fun single(request: IN): Single<OUT> {
      return rxAsk(request).asSingle()
   }

   infix open fun launch(request: IN): DeferredAction<OUT> {
      return provider.broker.launch(
         request = request,
         provider = provider,
         timeoutTicks = provider.timeoutTicks
      )
   }

   open fun launch(request: IN,
                   timeout: Long,
                   unit: TimeUnit = TimeUnit.MILLISECONDS): DeferredAction<OUT> {
      return provider.broker.launch(
         request = request,
         provider = provider,
         timeoutTicks = MEventLoop.calculateTicks(timeout, unit)
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

   infix suspend open fun await(block: IN.() -> Unit): OUT {
      return ask(createRequest().apply(block))
   }

   infix open fun rxAsk(block: IN.() -> Unit): DeferredAction<OUT> {
      return rxAsk(createRequest().apply(block))
   }

   infix open fun launch(block: IN.() -> Unit): DeferredAction<OUT> {
      return launch(createRequest().apply(block))
   }

   @Deprecated("", ReplaceWith("launch"))
   open fun singleBuilder(request: IN): Single<OUT> {
      return single(request)
   }

   @Deprecated("", ReplaceWith("launch"))
   open fun singleBuilder(token: ActionToken?, request: IN): Single<OUT> {
      return provider.broker.launch(
         request = request,
         provider = provider,
         token = token ?: NoToken,
         timeoutTicks = provider.timeoutTicks
      ).asSingle()
   }

   @Deprecated("", ReplaceWith("rxAsk"))
   open fun singleBuilder(consumer: Consumer<IN>): Single<OUT> {
      val request = createRequest()
      consumer.accept(request)
      return single(request)
   }

   @Deprecated("", ReplaceWith("rxAsk"))
   open fun singleBuilder(token: ActionToken?, consumer: Consumer<IN>): Single<OUT> {
      val request = createRequest()
      consumer.accept(request)

      return provider.broker.launch(
         request = request,
         provider = provider,
         token = token ?: NoToken,
         timeoutTicks = provider.timeoutTicks
      ).asSingle()
   }
}


abstract class WorkerActionProducer
<A : JobAction<IN, OUT>, IN : Any, OUT : Any, P : WorkerActionProvider<A, IN, OUT>>
   : ActionProducer<A, IN, OUT, P>() {

   var guarded = true
      get
      private set

   override fun onProviderSet(provider: P) {
      super.onProviderSet(provider)
      guarded = provider.annotation?.guarded == true
   }

   @Deprecated("!!! THIS IS BLOCKING !!!")
   open infix fun execute(request: IN): OUT {
      return provider.broker.rxAsk(
         request = request,
         provider = provider,
         timeoutTicks = provider.timeoutTicks
      ).asSingle().toBlocking().value()
   }

   @Deprecated("!!! THIS IS BLOCKING !!!")
   open infix fun blocking(request: IN): OUT {
      return provider.broker.rxAsk(
         request = request,
         provider = provider,
         timeoutTicks = provider.timeoutTicks
      ).asSingle().toBlocking().value()
   }

   @Deprecated("!!! THIS IS BLOCKING !!!")
   open infix fun blockingLocal(request: IN): OUT {
      return provider.broker.rxAsk(
         request = request,
         provider = provider,
         timeoutTicks = provider.timeoutTicks
      ).asSingle().toBlocking().value()
   }

   /**
    * Waits for response.
    * This executes as a child to the current action.
    *
    * Exception is thrown if it times out.
    */
   suspend open infix fun await(request: IN): OUT {
      return provider.broker.ask(
         request = request,
         provider = provider,
         timeoutTicks = provider.timeoutTicks
      )
   }

   /**
    * Waits for response.
    * This executes as a child to the current action.
    *
    * Exception is thrown if it times out.
    */
   suspend open infix fun ask(request: IN): OUT {
      return provider.broker.ask(
         request = request,
         provider = provider,
         timeoutTicks = provider.timeoutTicks
      )
   }

   suspend open fun ask(request: IN,
                        timeout: Long = 0,
                        unit: TimeUnit = TimeUnit.MILLISECONDS): OUT {
      return provider.broker.ask(
         request = request,
         provider = provider,
         timeoutTicks = MEventLoop.calculateTicks(timeout, unit)
      )
   }

   infix fun rxAsk(request: IN): DeferredAction<OUT> {
      return provider.broker.rxAsk(
         request = request,
         provider = provider,
         timeoutTicks = provider.timeoutTicks
      )
   }

   fun rxAsk(request: IN, actionProvider: Provider<A>): DeferredAction<OUT> {
      return provider.broker.rxAsk(
         request = request,
         provider = provider,
         timeoutTicks = provider.timeoutTicks
      )
   }

   fun rxAsk(request: IN,
             timeout: Long = 0,
             unit: TimeUnit = TimeUnit.MILLISECONDS): DeferredAction<OUT> {

      return provider.broker.rxAsk(
         request = request,
         provider = provider,
         timeoutTicks = when (timeout) {
            -1L -> -1L
            0L -> provider.timeoutTicks
            else -> unit.toMillis(timeout)
         }
      )
   }

   fun request(request: IN,
               timeout: Long = 0,
               unit: TimeUnit = TimeUnit.MILLISECONDS) =
      AskMessage(
         request,
         ASK_ASYNC,
         when (timeout) {
            -1L -> -1L
            0L -> provider.timeoutTicks
            else -> unit.toMillis(timeout)
         },
         this
      )

   open operator fun invoke(request: IN) =
      AskMessage(
         request,
         ASK_ASYNC,
         provider.timeoutTicks,
         this
      )

   open operator fun invoke(request: IN,
                            timeout: Long = 0,
                            unit: TimeUnit = TimeUnit.MILLISECONDS) =
      AskMessage(
         request,
         ASK_ASYNC,
         when (timeout) {
            -1L -> -1L
            0L -> provider.timeoutTicks
            else -> unit.toMillis(timeout)
         },
         this
      )

   fun fifo(request: IN,
            timeout: Long = 0,
            unit: TimeUnit = TimeUnit.MILLISECONDS) =
      AskMessage(
         request,
         ASK_FIFO,
         when (timeout) {
            -1L -> -1L
            0L -> provider.timeoutTicks
            else -> unit.toMillis(timeout)
         },
         this
      )

   @Deprecated("", ReplaceWith("rxAsk"))
   open fun single(request: IN): Single<OUT> {
      return rxAsk(request).asSingle()
   }

   @Deprecated("", ReplaceWith("rxAsk"))
   open fun single(token: ActionToken?, request: IN): Single<OUT> {
      return provider.broker.launch(
         request = request,
         provider = provider,
         token = token ?: NoToken,
         timeoutTicks = provider.timeoutTicks
      ).asSingle()
   }

   @Deprecated("", ReplaceWith("launch"))
   infix fun send(request: IN): Single<WorkerReceipt> {
      launch(request)
      return Single.just(WorkerReceipt().apply { messageId = NUID.nextGlobal() })
   }

   @Deprecated("", ReplaceWith("launch"))
   fun send(request: IN, key: String): Single<WorkerReceipt> {
      launch(request)
      return Single.just(WorkerReceipt().apply { messageId = NUID.nextGlobal() })
   }

   infix fun launch(request: IN): DeferredAction<OUT> {
      return provider.broker.launch(
         request = request,
         provider = provider,
         timeoutTicks = provider.timeoutTicks
      )
   }

   fun launch(request: IN,
              timeout: Long,
              unit: TimeUnit = TimeUnit.MILLISECONDS): DeferredAction<OUT> {
      return provider.broker.launch(
         request = request,
         provider = provider,
         timeoutTicks = MEventLoop.calculateTicks(timeout, unit)
      )
   }

   fun launch(request: IN,
              token: ActionToken?,
              timeout: Long,
              unit: TimeUnit = TimeUnit.MILLISECONDS): DeferredAction<OUT> {
      return provider.broker.launch(
         request = request,
         provider = provider,
         token = token ?: NoToken,
         timeoutTicks = MEventLoop.calculateTicks(timeout, unit)
      )
   }

   open fun launchFromJson(requestJson: String): DeferredAction<OUT> {
      val request = Wire.parse(provider.requestClass, requestJson)

      return provider.broker.launch(
         request = request,
         provider = provider,
         token = NoToken,
         timeoutTicks = provider.timeoutTicks
      )
   }

   open fun launchFromJson(message: ByteArray,
                           token: ActionToken? = NoToken): DeferredAction<OUT> {
      val request = Wire.parse(provider.requestClass, message)

      return provider.broker.launch(
         request = request,
         provider = provider,
         token = token ?: NoToken,
         timeoutTicks = provider.timeoutTicks
      )
   }

   open fun launchFromJson(message: String,
                           token: ActionToken? = NoToken): DeferredAction<OUT> {
      val request = Wire.parse(provider.requestClass, message)

      return provider.broker.launch(
         request = request,
         provider = provider,
         token = token ?: NoToken,
         timeoutTicks = provider.timeoutTicks
      )
   }

   open fun launchFromJson(message: String,
                           token: ActionToken?,
                           timeout: Long,
                           unit: TimeUnit = TimeUnit.MILLISECONDS): DeferredAction<OUT> {
      val request = Wire.parse(provider.requestClass, message)

      return provider.broker.launch(
         request = request,
         provider = provider,
         token = token ?: NoToken,
         timeoutTicks = MEventLoop.calculateTicks(timeout, unit)
      )
   }

   open fun launchFromJson(message: ByteArray,
                           token: ActionToken?,
                           timeout: Long,
                           unit: TimeUnit = TimeUnit.MILLISECONDS): DeferredAction<OUT> {
      val request = Wire.parse(provider.requestClass, message)

      return provider.broker.launch(
         request = request,
         provider = provider,
         token = token ?: NoToken,
         timeoutTicks = MEventLoop.calculateTicks(timeout, unit)
      )
   }

   open fun launchFromJson(message: ByteBuf,
                           token: ActionToken?,
                           timeout: Long,
                           unit: TimeUnit = TimeUnit.MILLISECONDS): DeferredAction<OUT> {
      val request = Wire.parse(provider.requestClass, message)

      return provider.broker.launch(
         request = request,
         provider = provider,
         token = token ?: NoToken,
         timeoutTicks = MEventLoop.calculateTicks(timeout, unit)
      )
   }


   open fun launchFromMsgPack(message: ByteArray): DeferredAction<OUT> {
      val request = Wire.unpack(provider.requestClass, message)

      return provider.broker.launch(
         request = request,
         provider = provider,
         token = NoToken,
         timeoutTicks = provider.timeoutTicks
      )
   }

   open fun launchFromMsgPack(message: ByteBuf): DeferredAction<OUT> {
      val request = Wire.unpack(provider.requestClass, message)

      return provider.broker.launch(
         request = request,
         provider = provider,
         token = NoToken,
         timeoutTicks = provider.timeoutTicks
      )
   }

   open fun launchFromMsgPack(message: ByteArray,
                              token: ActionToken? = NoToken): DeferredAction<OUT> {
      val request = Wire.unpack(provider.requestClass, message)

      return provider.broker.launch(
         request = request,
         provider = provider,
         token = token ?: NoToken,
         timeoutTicks = provider.timeoutTicks
      )
   }

   open fun launchFromMsgPack(message: ByteBuf,
                              token: ActionToken? = NoToken): DeferredAction<OUT> {
      val request = Wire.unpack(provider.requestClass, message)

      return provider.broker.launch(
         request = request,
         provider = provider,
         token = token ?: NoToken,
         timeoutTicks = provider.timeoutTicks
      )
   }

   open fun launchFromMsgPack(message: ByteArray,
                              token: ActionToken?,
                              timeout: Long,
                              unit: TimeUnit = TimeUnit.MILLISECONDS): DeferredAction<OUT> {
      val request = Wire.unpack(provider.requestClass, message)

      return provider.broker.launch(
         request = request,
         provider = provider,
         token = token ?: NoToken,
         timeoutTicks = MEventLoop.calculateTicks(timeout, unit)
      )
   }

   open fun launchFromMsgPack(message: ByteBuf,
                              token: ActionToken,
                              timeout: Long,
                              unit: TimeUnit = TimeUnit.MILLISECONDS): DeferredAction<OUT> {
      val request = Wire.unpack(provider.requestClass, message)

      return provider.broker.launch(
         request = request,
         provider = provider,
         token = token,
         timeoutTicks = MEventLoop.calculateTicks(timeout, unit)
      )
   }

//   open fun launchFromMsgPackToMsgPack(message: ByteBuf,
//                              token: ActionToken,
//                              timeout: Long,
//                              unit: TimeUnit = TimeUnit.MILLISECONDS): DeferredAction<ByteBuf> {
//      val request = Wire.unpack(provider.requestClass, message)
//
//      return provider.broker.launch(
//         request = request,
//         provider = provider,
//         token = token,
//         timeoutTicks = MEventLoop.calculateTicks(timeout, unit)
//      )
//   }

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

   operator fun invoke(block: IN.() -> Unit): AskMessage<A, IN, OUT, P> {
      return super.invoke(createRequest().apply(block))
   }

   suspend infix open fun ask(block: IN.() -> Unit): OUT {
      return ask(createRequest().apply(block))
   }

   suspend infix open fun await(block: IN.() -> Unit): OUT {
      return ask(createRequest().apply(block))
   }

   infix open fun rxAsk(block: IN.() -> Unit): DeferredAction<OUT> {
      return rxAsk(createRequest().apply(block))
   }

   infix open fun launch(block: IN.() -> Unit): DeferredAction<OUT> {
      return launch(createRequest().apply(block))
   }

//   @Deprecated("", ReplaceWith("rxAsk"))
//   open fun singleBuilder(request: IN): Single<OUT> {
//      return single(request)
//   }

   @Deprecated("", ReplaceWith("launch"))
   open fun singleBuilder(request: IN): Single<OUT> {
      return single(request)
   }

   @Deprecated("", ReplaceWith("launch"))
   open fun singleBuilder(token: ActionToken?, request: IN): Single<OUT> {
      return provider.broker.launch(
         request = request,
         provider = provider,
         token = token ?: NoToken,
         timeoutTicks = provider.timeoutTicks
      ).asSingle()
   }

   @Deprecated("", ReplaceWith("rxAsk"))
   open fun singleBuilder(consumer: Consumer<IN>): Single<OUT> {
      val request = createRequest()
      consumer.accept(request)
      return single(request)
   }

   @Deprecated("", ReplaceWith("rxAsk"))
   open fun singleBuilder(token: ActionToken?, consumer: Consumer<IN>): Single<OUT> {
      val request = createRequest()
      consumer.accept(request)

      return provider.broker.launch(
         request = request,
         provider = provider,
         token = token ?: NoToken,
         timeoutTicks = provider.timeoutTicks
      ).asSingle()
   }

   @Deprecated("", ReplaceWith("launch"))
   infix fun send(consumer: Consumer<IN>): Single<WorkerReceipt> {
      val request = createRequest()
      consumer.accept(request)
      launch(request)
      return Single.just(WorkerReceipt().apply { messageId = NUID.nextGlobal() })
   }

   override fun launchFromJson(message: String,
                               token: ActionToken?,
                               timeout: Long,
                               unit: TimeUnit): DeferredAction<OUT> {
      val request = if (message.isNotEmpty()) {
         Wire.parse(provider.requestClass, message)
      } else {
         createRequest()
      }

      return provider.broker.launch(
         request = request,
         provider = provider,
         token = token ?: NoToken,
         timeoutTicks = MEventLoop.calculateTicks(timeout, unit)
      )
   }

   override fun launchFromJson(message: ByteArray,
                               token: ActionToken?,
                               timeout: Long,
                               unit: TimeUnit): DeferredAction<OUT> {
      val request = if (message.isNotEmpty()) {
         Wire.parse(provider.requestClass, message)
      } else {
         createRequest()
      }

      return provider.broker.launch(
         request = request,
         provider = provider,
         token = token ?: NoToken,
         timeoutTicks = MEventLoop.calculateTicks(timeout, unit)
      )
   }

   override fun launchFromJson(message: ByteBuf,
                               token: ActionToken?,
                               timeout: Long,
                               unit: TimeUnit): DeferredAction<OUT> {
      val request = if (message.isReadable) {
         Wire.parse(provider.requestClass, message)
      } else {
         createRequest()
      }

      return provider.broker.launch(
         request = request,
         provider = provider,
         token = token ?: NoToken,
         timeoutTicks = MEventLoop.calculateTicks(timeout, unit)
      )
   }

   override fun launchFromMsgPack(message: ByteArray,
                                  token: ActionToken?,
                                  timeout: Long,
                                  unit: TimeUnit): DeferredAction<OUT> {
      val request = Wire.unpack(provider.requestClass, message)

      return provider.broker.launch(
         request = request,
         provider = provider,
         token = token ?: NoToken,
         timeoutTicks = MEventLoop.calculateTicks(timeout, unit)
      )
   }

   override fun launchFromMsgPack(message: ByteBuf,
                                  token: ActionToken,
                                  timeout: Long,
                                  unit: TimeUnit): DeferredAction<OUT> {
      val request = Wire.unpack(provider.requestClass, message)

      return provider.broker.launch(
         request = request,
         provider = provider,
         token = token,
         timeoutTicks = MEventLoop.calculateTicks(timeout, unit)
      )
   }
}


abstract class HttpActionProducer
<A : HttpAction, P : HttpActionProvider<A>>
   : ActionProducer<A, RoutingContext, Unit, P>() {

   fun visibleTo(visibility: ActionVisibility) = provider.visibility == visibility

   /**
    * Waits for response.
    * This executes as a child to the current action.
    *
    * Exception is thrown if it times out.
    */
   suspend open infix fun ask(request: RoutingContext) {
      provider.broker.ask(
         request = request,
         provider = provider,
         timeoutTicks = provider.timeoutTicks
      )
   }

   suspend open fun ask(request: RoutingContext,
                        timeout: Long = 0,
                        unit: TimeUnit = TimeUnit.MILLISECONDS) {
      return provider.broker.ask(
         request = request,
         provider = provider,
         timeoutTicks = MEventLoop.calculateTicks(timeout, unit)
      )
   }

   infix fun rxAsk(request: RoutingContext): DeferredAction<Unit> {
      return provider.broker.rxAsk(
         request = request,
         provider = provider,
         timeoutTicks = provider.timeoutTicks
      )
   }

   @Deprecated("", ReplaceWith("rxAsk"))
   fun single(request: RoutingContext): Single<Unit> {
      return rxAsk(request).asSingle()
   }

   infix fun launch(request: RoutingContext): DeferredAction<Unit> {
      return provider.broker.launch(
         request = request,
         provider = provider,
         timeoutTicks = provider.timeoutTicks
      )
   }

   fun launch(request: RoutingContext,
              timeout: Long,
              unit: TimeUnit = TimeUnit.MILLISECONDS): DeferredAction<Unit> {
      return provider.broker.launch(
         request = request,
         provider = provider,
         timeoutTicks = MEventLoop.calculateTicks(timeout, unit)
      )
   }
}