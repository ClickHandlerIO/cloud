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

   val actionProviders = registry.actionProviders
   val actionProducers = registry.actionProducers
   val providersMap by lazy { registry.actionProviders.map { it.actionClass to it }.toMap() }
   val providersNameMap by lazy { registry.actionProviders.map { it.actionClass.canonicalName to it }.toMap() }
   val producersMap by lazy { registry.actionProducers.map { it.provider.actionClass to it }.toMap() }

   val internal by lazy {
      actionProducers
         .filter { it is InternalActionProducer }
         .map { it as InternalActionProducer<*, *, *, *> }
   }

   val worker by lazy {
      actionProducers
         .filter { it is WorkerActionProducer }
         .map { it as WorkerActionProducer<*, *, *, *> }
   }

   val http by lazy {
      actionProducers
         .filter { it is HttpActionProducer }
         .map { it as HttpActionProducer<*, *> }
   }

   val actors by lazy {
      registry.actorProducers
         .filter { it is ActorProducer<*, *> && it !is DaemonProducer<*, *> }
         .map { it as ActorProducer<*, *> }
   }

   val daemons by lazy {
      registry.actorProducers
         .filter { it is DaemonProducer<*, *> }
         .map { it as DaemonProducer<*, *> }
         .filter {
            when (it.provider.role) {
               NodeRole.REMOTE -> ROLE_REMOTE
               NodeRole.WORKER -> ROLE_WORKER
               else -> true
            }
         }
         .sortedBy { it.provider.sort }
   }

   val workersByClass by lazy {
      worker
         .filter { !it.provider.annotation.path.isNullOrBlank() }
         .map { it.provider.actionClass to it }
         .toMap()
   }

   val publicWorkers by lazy {
      worker
         .filter { !it.provider.annotation.path.isNullOrBlank() }
         .filter { it.provider.annotation.visibility == ActionVisibility.PUBLIC }
   }

   val publicWorkersByClass by lazy {
      worker
         .filter { !it.provider.annotation.path.isNullOrBlank() }
         .filter { it.provider.annotation.visibility == ActionVisibility.PUBLIC }
         .map { it.provider.actionClass to it }
         .toMap()
   }

   val workersByRequestClass by lazy {
      worker
         .filter { !it.provider.annotation.path.isNullOrBlank() }
         .map { it.provider.requestClass to it }
         .toMap()
   }

   val workersByReplyClass by lazy {
      worker
         .filter { !it.provider.annotation.path.isNullOrBlank() }
         .map { it.provider.requestClass to it }
         .toMap()
   }

   val workersByPath by lazy {
      worker
         .filter { !it.provider.annotation.path.isNullOrBlank() }
         .map { it.provider.annotation.path to it }
         .toMap()
   }

   val workersByName by lazy {
      worker
         .map { it.provider.actionClass.canonicalName to it }
         .toMap()
   }

   val httpByPath by lazy {
      http
         .filter { !it.provider.annotation.path.isNullOrBlank() }
         .map { it.provider.annotation.path to it }
         .toMap()
   }
}
