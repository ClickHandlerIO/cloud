package move.action

import javax.inject.Inject

/**

 */
@ActionConfig
@InternalAction
class Allocate @Inject
constructor() : Action<String, String>() {
   suspend override fun recover(caught: Throwable, cause: Throwable, isFallback: Boolean): String {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   suspend override fun execute(): String {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }
}
