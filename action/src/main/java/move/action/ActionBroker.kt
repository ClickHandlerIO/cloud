package move.action

import io.vertx.ext.web.RoutingContext
import io.vertx.rxjava.core.Vertx
import kotlinx.coroutines.experimental.Deferred
import java.util.concurrent.TimeUnit

/**
 *
 */
val LOCAL_BROKER = LocalBroker

var _GATEWAY_BROKER: ActionBroker = LOCAL_BROKER

fun changeGatewayBroker(broker: ActionBroker) {
   _GATEWAY_BROKER = broker
}

/**
 *
 */
val GATEWAY_BROKER
   get() = _GATEWAY_BROKER

/**
 * In charge of creating, scheduling. queuing, logging, and invoking Actions.
 *
 * Each node has a single queue
 */
class ActionKernel(val vertx: Vertx) {
   val eventLoopGroup = MoveEventLoopGroup.get(vertx)
   val eventLoops = eventLoopGroup.executors

   fun invokeInternal(provider: InternalActionProvider<*, *, *>) {

   }

   fun invokeWorker(provider: WorkerActionProvider<*, *, *>) {

   }
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
      provider: InternalActionProvider<A, IN, OUT>,
      timeout: Long = 0,
      unit: TimeUnit = TimeUnit.MILLISECONDS,
      delaySeconds: Int = 0,
      root: Boolean = false
   ): OUT

   /**
    *
    */
   abstract fun <A : InternalAction<IN, OUT>, IN : Any, OUT : Any> rxAsk(
      request: IN,
      provider: InternalActionProvider<A, IN, OUT>,
      timeout: Long = 0,
      unit: TimeUnit = TimeUnit.MILLISECONDS,
      delaySeconds: Int = 0,
      root: Boolean = false
   ): Deferred<OUT>

   /**
    *
    */
   abstract suspend fun <A : HttpAction> ask(
      request: RoutingContext,
      provider: HttpActionProvider<A>,
      timeout: Long = 0,
      unit: TimeUnit = TimeUnit.MILLISECONDS,
      delaySeconds: Int = 0,
      root: Boolean = false
   )

   /**
    *
    */
   abstract fun <A : HttpAction> rxAsk(
      request: RoutingContext,
      provider: HttpActionProvider<A>,
      timeout: Long = 0,
      unit: TimeUnit = TimeUnit.MILLISECONDS,
      delaySeconds: Int = 0,
      root: Boolean = false
   ): JobAction<RoutingContext, Unit>

//   /**
//    *
//    */
//   abstract fun <A : InternalAction<IN, OUT>, IN : Any, OUT : Any> rxAsk(
//      request: IN,
//      provider: InternalActionProvider<A, IN, OUT>,
//      timeout: Long = 0,
//      unit: TimeUnit = TimeUnit.SECONDS,
//      delaySeconds: Int = 0,
//      root: Boolean = false
//   ): io.reactivex.Single<OUT>

   /**
    *
    */
   //   abstract suspend fun <A : InternalAction<IN, OUT>, IN : Any, OUT : Any> tell(
//      request: IN,
//      provider: InternalActionProvider<A, IN, OUT>,
//      timeout: Long = 0,
//      unit: TimeUnit = TimeUnit.SECONDS,
//      delaySeconds: Int = 0,
//      root: Boolean = false
//   ): BrokerReceipt<OUT>

   /**
    *
    */
   abstract suspend fun <A : WorkerAction<IN, OUT>, IN : Any, OUT : Any> ask(
      request: IN,
      provider: WorkerActionProvider<A, IN, OUT>,
      timeout: Long = 0,
      unit: TimeUnit = TimeUnit.MILLISECONDS,
      delaySeconds: Int = 0,
      root: Boolean = false
   ): OUT

//   /**
//    *
//    */
//   abstract fun <A : WorkerAction<IN, OUT>, IN : Any, OUT : Any> rxAsk(
//      request: IN,
//      provider: WorkerActionProvider<A, IN, OUT>,
//      timeout: Long = 0,
//      unit: TimeUnit = TimeUnit.SECONDS,
//      delaySeconds: Int = 0,
//      root: Boolean = false
//   ): io.reactivex.Single<OUT>

   /**
    *
    */
   //   abstract suspend fun <A : WorkerAction<IN, OUT>, IN : Any, OUT : Any> tell(
//      request: IN,
//      provider: WorkerActionProvider<A, IN, OUT>,
//      timeout: Long = 0,
//      unit: TimeUnit = TimeUnit.SECONDS,
//      delaySeconds: Int = 0,
//      root: Boolean = false
//   ): Single<BrokerReceipt<OUT>>

   companion object {
      internal val _default = LocalBroker

      val DEFAULT: ActionBroker
         get() = _default
   }
}

/**
 *
 */
object LocalBroker : ActionBroker() {
   override val id: Int
      get() = 0

   suspend override fun <A : InternalAction<IN, OUT>, IN : Any, OUT : Any> ask(
      request: IN,
      provider: InternalActionProvider<A, IN, OUT>,
      timeout: Long,
      unit: TimeUnit,
      delaySeconds: Int,
      root: Boolean): OUT {

      return if (root) {
         provider.createAsRoot(request, timeout, unit).await()
      } else {
         provider.createAsChild(request, timeout, unit).await()
      }
   }

   override fun <A : InternalAction<IN, OUT>, IN : Any, OUT : Any> rxAsk(request: IN, provider: InternalActionProvider<A, IN, OUT>, timeout: Long, unit: TimeUnit, delaySeconds: Int, root: Boolean): Deferred<OUT> {
      return if (root) {
         provider.createAsRoot(request, timeout, unit)
      } else {
         provider.createAsChild(request, timeout, unit)
      }
   }

   suspend override fun <A : HttpAction> ask(request: RoutingContext, provider: HttpActionProvider<A>, timeout: Long, unit: TimeUnit, delaySeconds: Int, root: Boolean) {
      if (root) {
         provider.createAsRoot(request, timeout, unit).await()
      } else {
         provider.createAsChild(request, timeout, unit).await()
      }
   }

   override fun <A : HttpAction> rxAsk(request: RoutingContext, provider: HttpActionProvider<A>, timeout: Long, unit: TimeUnit, delaySeconds: Int, root: Boolean): JobAction<RoutingContext, Unit> {
      return if (root) {
         provider.createAsRoot(request, timeout, unit)
      } else {
         provider.createAsChild(request, timeout, unit)
      }
   }

   //   override fun <A : InternalAction<IN, OUT>, IN : Any, OUT : Any> rxAsk(request: IN, provider: InternalActionProvider<A, IN, OUT>, timeout: Long, unit: TimeUnit, delaySeconds: Int, root: Boolean): io.reactivex.Single<OUT> {
//      return if (root) {
//         provider.createAsRoot(request, timeout, unit).asSingle()
//      } else {
//         provider.createAsChild(request, timeout, unit).asSingle()
//      }
//   }

   suspend override fun <A : WorkerAction<IN, OUT>, IN : Any, OUT : Any> ask(request: IN, provider: WorkerActionProvider<A, IN, OUT>, timeout: Long, unit: TimeUnit, delaySeconds: Int, root: Boolean): OUT {
      return if (root) {
         provider.createAsRoot(request, timeout, unit).await()
      } else {
         provider.createAsChild(request, timeout, unit).await()
      }
   }

//   override fun <A : WorkerAction<IN, OUT>, IN : Any, OUT : Any> rxAsk(request: IN, provider: WorkerActionProvider<A, IN, OUT>, timeout: Long, unit: TimeUnit, delaySeconds: Int, root: Boolean): io.reactivex.Single<OUT> {
//      return if (root) {
//         provider.createAsRoot(request, timeout, unit).asSingle()
//      } else {
//         provider.createAsChild(request, timeout, unit).asSingle()
//      }
//   }

//   /**
//    *
//    */
//   class Receipt<T>(rx: Single<T>, id: String = NUID.nextGlobal()) : BrokerReceipt<T>(rx, id) {
//      override val nodeId: String
//         get() = ""
//      override val brokerId: String
//         get() = id
//   }
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