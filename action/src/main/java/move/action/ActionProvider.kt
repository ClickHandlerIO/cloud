package move.action

import io.vertx.kotlin.circuitbreaker.CircuitBreakerOptions
import io.vertx.rxjava.core.Vertx
import javax.inject.Provider
import kotlin.reflect.KProperty

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

   val vertxCore: io.vertx.core.Vertx = vertx.delegate
   val eventLoopGroup = MoveEventLoopGroup.get(vertx)

   open val isInternal = false
   open val isWorker = false
   open val isHttp = false
   open val isDaemon = false

   var name: String = findName("")
      get
      internal set

   var breaker: ActionCircuitBreaker = ActionCircuitBreaker(
      actionClass.name,
      vertx.delegate,
      CircuitBreakerOptions()
         .setMetricsRollingWindow(5000L)
         .setResetTimeout(2000L),
      eventLoopGroup
   )

   var broker: ActionBroker by BrokerDelegate()

   class BrokerDelegate {
      var value = ActionBroker.DEFAULT

      operator fun getValue(thisRef: Any?, property: KProperty<*>): ActionBroker {
         return value
      }

      operator fun setValue(thisRef: Any?, property: KProperty<*>, value: ActionBroker) {
         this.value = value
      }
   }

   init {
      init()
   }

   fun findName(fromAnnotation: String?): String {
      if (fromAnnotation?.isNotBlank() == true) {
         return fromAnnotation.trim()
      }
      val n = actionClass.canonicalName
      if (n.startsWith("action.")) {
         return n.substring("action.".length)
      }
      return n
   }

   protected open fun init() {
   }

   companion object {
      internal val MIN_TIMEOUT_MILLIS = 200
      internal val MILLIS_TO_SECONDS_THRESHOLD = 500
   }
}
