package move.action

import javax.inject.Inject
import javax.inject.Singleton

/**
 * A registry of all Actions in this App instance.
 * This is automatically created by the ActionProcessor
 * and dagger.
 */
@Singleton
class ActionStore
@Inject
constructor(val registry: ActionRegistry) {

   val providers = registry.providers
   val producers = registry.producers
   val providersMap by lazy { registry.providers.map { it.actionClass to it }.toMap() }
   val providersNameMap by lazy { registry.providers.map { it.actionClass.canonicalName to it }.toMap() }
   val producersMap by lazy { registry.producers.map { it.provider.actionClass to it }.toMap() }

   val internal by lazy {
      producers
         .filter { it is InternalActionProducer }
         .map { it as InternalActionProducer<*, *, *, *> }
   }

   val worker by lazy {
      producers
         .filter { it is WorkerActionProducer }
         .map { it as WorkerActionProducer<*, *, *, *> }
   }

   val http by lazy {
      producers
         .filter { it is HttpActionProducer }
         .map { it as HttpActionProducer<*, *> }
   }

   val daemons by lazy {
      producers
         .filter { it is HttpActionProducer }
         .map { it as HttpActionProducer<*, *> }
   }

   val workersByClass by lazy {
      worker
         .filter { !it.provider.annotation?.path.isNullOrBlank() }
         .map { it.provider.actionClass to it }
         .toMap()
   }

   val publicWorkers by lazy {
      worker
         .filter { !it.provider.annotation?.path.isNullOrBlank() }
         .filter { it.provider.annotation?.visibility == ActionVisibility.PUBLIC }
   }

   val publicWorkersByClass by lazy {
      worker
         .filter { !it.provider.annotation?.path.isNullOrBlank() }
         .filter { it.provider.annotation?.visibility == ActionVisibility.PUBLIC }
         .map { it.provider.actionClass to it }
         .toMap()
   }

   val workersByRequestClass by lazy {
      worker
         .filter { !it.provider.annotation?.path.isNullOrBlank() }
         .map { it.provider.requestClass to it }
         .toMap()
   }

   val workersByReplyClass by lazy {
      worker
         .filter { !it.provider.annotation?.path.isNullOrBlank() }
         .map { it.provider.requestClass to it }
         .toMap()
   }

   val workersByPath by lazy {
      worker
         .filter { !it.provider.annotation?.path.isNullOrBlank() }
         .map { it.provider.annotation?.path to it }
         .toMap()
   }

   val workersByName by lazy {
      worker
         .map { it.provider.actionClass.canonicalName to it }
         .toMap()
   }

   val httpByPath by lazy {
      http
         .filter { !it.provider.annotation?.path.isNullOrBlank() }
         .map { it.provider.annotation?.path to it }
         .toMap()
   }
}
