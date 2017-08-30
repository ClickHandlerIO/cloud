package move.action

import move.action._Move_Component.Builder
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 *
 */
@Singleton
class ActionStore @Inject constructor(
   val manager: ActionManager,
   builderProvider: Provider<Builder>) {

   val component: _Move_Component = builderProvider.get().build()

   init {
      ActionManager.register(component)

      // Setup Action Brokers.
      // Multiple Action Brokers may be used.
   }

   fun init() = Unit
}

object ActionBrokers {
   val GCLOUD = 0
   val SQS = 1
   val MOVECLOUD = 2

   val DEFAULT_BROKER = SQS
}
