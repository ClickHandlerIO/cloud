package move.action

import io.netty.buffer.ByteBuf
import io.vertx.core.buffer.Buffer
import io.vertx.kotlin.circuitbreaker.CircuitBreakerOptions
import io.vertx.rxjava.core.Vertx
import kotlinx.coroutines.experimental.rx1.await
import move.common.WireFormat
import rx.Single
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import javax.inject.Provider

/**
 * Builds and invokes a single type of Action.

 * @author Clay Molocznik
 */
abstract class ActionProvider<A : Action<IN, OUT>, IN : Any, OUT : Any>
constructor(val vertx: Vertx, val actionProvider: Provider<A>) {
   val actionProviderClass = TypeResolver
      .resolveRawClass(
         ActionProvider::class.java,
         javaClass
      )

   @Suppress("UNCHECKED_CAST")
   val actionClass = TypeResolver
      .resolveRawArgument(
         actionProviderClass.typeParameters[0],
         javaClass
      ) as Class<A>

   @Suppress("UNCHECKED_CAST")
   val requestClass = TypeResolver
      .resolveRawArgument(
         actionProviderClass.typeParameters[1],
         javaClass
      ) as Class<IN>

   @Suppress("UNCHECKED_CAST")
   val replyClass = TypeResolver.resolveRawArgument(
      actionProviderClass.typeParameters[2],
      javaClass
   ) as Class<OUT>

   @Suppress("UNCHECKED_CAST")
   val self = this as ActionProvider<Action<IN, OUT>, IN, OUT>

   abstract val annotationTimeout: Int

   val vertxCore: io.vertx.core.Vertx = vertx.delegate

   val eventLoopGroup = ActionEventLoopGroup.get(vertx)

   val visibility
      get() = Remote.Visibility.PRIVATE

   private var executionTimeoutEnabled: Boolean = false
   private var timeoutMillis: Int = annotationTimeout
   var maxConcurrentRequests: Int = 0
      internal set

   var isExecutionTimeoutEnabled: Boolean
      get() = executionTimeoutEnabled
      set(enabled) {
      }

   var breaker: ActionCircuitBreaker = ActionCircuitBreaker(
      actionClass.name,
      vertx.delegate,
      CircuitBreakerOptions()
         .setMetricsRollingWindow(5000L)
         .setResetTimeout(2000L),
      eventLoopGroup
   )

   init {
      init()
   }

   fun getTimeoutMillis(): Long {
      return timeoutMillis.toLong()
   }

   fun timeoutMillis() = timeoutMillis

   open val isInternal = false
   open val isRemote = false
   open val isWorker = false
   open val isScheduled = false

   protected open fun init() {
      // Timeout milliseconds.
      var timeoutMillis = 0
      if (this.timeoutMillis > 0L) {
         timeoutMillis = this.timeoutMillis
      }

      this.timeoutMillis = timeoutMillis

      // Enable deadline?
      if (timeoutMillis > 0) {
         if (timeoutMillis < MILLIS_TO_SECONDS_THRESHOLD) {
            // Looks like somebody decided to put seconds instead of milliseconds.
            timeoutMillis = timeoutMillis * 1000
         } else if (timeoutMillis < 1000) {
            timeoutMillis = 1000
         }

         this.timeoutMillis = timeoutMillis
         executionTimeoutEnabled = true
      }
   }

   protected fun calcTimeout(action: A, context: ActionContext): Long {
      if (timeoutMillis < 1L) {
         return 0L
      }

      // Calculate max execution millis.
      val timesOutAt = System.currentTimeMillis() + timeoutMillis
      if (timesOutAt > context.currentTimeout) {
         return context.currentTimeout
      }

      return timesOutAt
   }

   fun create(): A {
      // Create new Action instance.
      val action = actionProvider.get()

      // Get or create ActionContext.
      var context: ActionContext? = Action.contextLocal.get()

      val timesOutAt: Long
      if (context == null) {
         val eventLoop: ActionEventLoopContext = eventLoopGroup.next()

         context = ActionContext(
            System.currentTimeMillis(),
            timeoutMillis.toLong(),
            this,
            eventLoop
         )
         timesOutAt = context.timesOutAt
      } else {
         timesOutAt = calcTimeout(action, context)
      }

      action.init(
         self,
         context,
         timesOutAt
      )

      // Return action instance.
      return action
   }

   internal fun create(request: IN): A {
      // Create new Action instance.
      val action = create()
      action._request = request

      // Return action instance.
      return action
   }

   internal fun create(data: Any?, request: IN): A {
      // Create new Action instance.
      val action = create()
      action._request = request
      action.context.data = data

      // Return action instance.
      return action
   }

   open fun rx(request: IN): Single<OUT> {
      return create().rx(request)
   }


   suspend internal fun call(request: IN,
                    timeoutUnit: TimeUnit = TimeUnit.SECONDS,
                    timeout: Long = 0,
                    broker: ActionBroker = ActionBroker.DEFAULT): OUT {
      if (isInternal) {
         return create().await(request)
      }

      // Send message through PubSub.
      // Set deadline to the timeout.
      // A node will not execute action if it goes past the deadline.

      // Optimize requiring an ACK based on past statistics.
      // If calls come back within 1 second we can save a message.


      // Wait until Response or Timeout expiring.

      return broker.call(request, this).await()
   }

   suspend internal fun rxCall(request: IN,
                    timeoutUnit: TimeUnit = TimeUnit.SECONDS,
                    timeout: Long = 0,
                    broker: ActionBroker = ActionBroker.DEFAULT): Single<OUT> {
      return broker.call(request, this)
   }


//   open fun requestAsMsgPack(request: IN): ByteArray {
//      return WireFormat.pack(request)
//   }
//
//   open fun requestAsJsonString(request: IN): String {
//      return WireFormat.stringify(request)
//   }
//
//   open fun requestAsJson(request: IN): ByteArray {
//      return WireFormat.byteify(request)
//   }
//
//   open fun requestFromMsgPack(buffer: ByteArray): IN {
//      return WireFormat.unpack(requestClass, buffer)
//   }
//
//   open fun requestFromMsgPack(buffer: Buffer): IN {
//      return WireFormat.unpack(requestClass, buffer)
//   }
//
//   open fun requestFromMsgPack(buffer: ByteBuffer): IN {
//      return WireFormat.unpack(requestClass, buffer)
//   }
//
//   open fun requestFromMsgPack(buffer: ByteBuf): IN {
//      return WireFormat.unpack(requestClass, buffer)
//   }
//
//   open fun requestFromJson(buffer: ByteArray): IN {
//      return WireFormat.parse(requestClass, buffer)
//   }
//
//   open fun requestFromJson(buffer: String): IN {
//      return WireFormat.parse(requestClass, buffer)
//   }
//
//   open fun requestFromJson(buffer: Buffer): IN {
//      return WireFormat.parse(requestClass, buffer)
//   }
//
//   open fun requestFromJson(buffer: ByteBuffer): IN {
//      return WireFormat.parse(requestClass, buffer)
//   }
//
//   open fun requestFromJson(buffer: ByteBuf): IN {
//      return WireFormat.parse(requestClass, buffer)
//   }
//
//
//   open fun replyAsMsgPack(request: OUT): ByteArray {
//      return WireFormat.pack(request)
//   }
//
//   open fun replyAsJson(request: OUT): ByteArray {
//      return WireFormat.byteify(request)
//   }
//
//   open fun replyAsJsonString(request: OUT): String {
//      return WireFormat.stringify(request)
//   }
//
//
//   open fun replyFromMsgPack(buffer: ByteArray): OUT {
//      return WireFormat.unpack(replyClass, buffer)
//   }
//
//   open fun replyFromMsgPack(buffer: Buffer): OUT {
//      return WireFormat.unpack(replyClass, buffer)
//   }
//
//   open fun replyFromMsgPack(buffer: ByteBuffer): OUT {
//      return WireFormat.unpack(replyClass, buffer)
//   }
//
//   open fun replyFromMsgPack(buffer: ByteBuf): OUT {
//      return WireFormat.unpack(replyClass, buffer)
//   }
//
//   open fun replyFromJson(buffer: ByteArray): OUT {
//      return WireFormat.parse(replyClass, buffer)
//   }
//
//   open fun replyFromJson(buffer: String): OUT {
//      return WireFormat.parse(replyClass, buffer)
//   }
//
//   open fun replyFromJson(buffer: Buffer): OUT {
//      return WireFormat.parse(replyClass, buffer)
//   }
//
//   open fun replyFromJson(buffer: ByteBuffer): OUT {
//      return WireFormat.parse(replyClass, buffer)
//   }
//
//   open fun replyFromJson(buffer: ByteBuf): OUT {
//      return WireFormat.parse(replyClass, buffer.nioBuffer())
//   }

   companion object {
      internal val MIN_TIMEOUT_MILLIS = 200
      internal val MILLIS_TO_SECONDS_THRESHOLD = 500
      internal val DEFAULT_CONCURRENCY_INTERNAL = 10000
      internal val DEFAULT_CONCURRENCY_REMOTE = 10000
      internal val DEFAULT_CONCURRENCY_WORKER = 10000
      internal val DEFAULT_CONCURRENCY_SCHEDULED = 1

      fun current(): ActionContext? {
         return Action.currentContext()
      }
   }
}
