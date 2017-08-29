package move.action

import move.action._ActionComponent_1.Builder
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 *
 */
@Singleton
class ActionStore @Inject
constructor(val manager: ActionManager, builderProvider: Provider<Builder>) {
   val component: _ActionComponent_1

   init {
      this.component = builderProvider.get().build()
      ActionManager.register(component)
   }

//   val root
//      get() = component.actions()
}
