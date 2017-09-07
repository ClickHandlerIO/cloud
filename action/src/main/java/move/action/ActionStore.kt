package move.action

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

/**
 * A registry of all Actions in this App instance.
 * This is automatically created by the ActionProcessor
 * and dagger.
 */
@Singleton
class ActionStore
@Inject
constructor(val registry: ActionRegistry) {

   val providersMap by lazy { registry.providers.map { it.actionClass to it }.toMap() }
   val providersNameMap by lazy { registry.providers.map { it.actionClass.canonicalName to it }.toMap() }
   val producersMap by lazy { registry.producers.map { it.provider.actionClass to it }.toMap() }

//   inline operator fun get(cls: KClass<HttpActionProducer<*, *>>) =
//      producers.filter { it is HttpActionProducer }.map { it as HttpActionProducer<*, *> }

   inline operator fun <reified P:Any> get(cls: KClass<P>) =
      registry.producers.filter { it is P }.map { it as P }

   val internal
      get() = registry.producers
         .filter { it is InternalActionProducer }
         .map { it as InternalActionProducer<*, *, *, *> }

   val worker
      get() = registry.producers
         .filter { it is WorkerActionProducer }
         .map { it as WorkerActionProducer<*, *, *, *> }

   val http
      get() = registry.producers
         .filter { it is HttpActionProducer }
         .map { it as HttpActionProducer<*, *> }
}
