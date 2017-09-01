package move.action

import io.vertx.rxjava.core.Vertx
import javax.inject.Inject
import javax.inject.Provider

/**
 * ActionProvider for Remote Actions.
 */
class RemoteActionProvider<A : Action<IN, OUT>, IN : Any, OUT : Any> @Inject
constructor(vertx: Vertx,
            actionProvider: Provider<A>) : ActionProvider<A, IN, OUT>(
   vertx, actionProvider
) {
   /**
    *
    */
   val remote: Remote? = actionClass.getAnnotation(Remote::class.java)

   override val annotationTimeout: Int
      get() = remote?.timeout ?: 0

   override val isRemote = true

   /**
    *
    */
   val isGuarded: Boolean
      get() = remote?.guarded ?: false

   lateinit var path: String

   init {
      path = remote?.path!!

      if (path.isNullOrBlank()) {
         var begin = false
         val p = actionClass.canonicalName
         var newP = ""

         p.split(".").forEach {
            if (begin) {
               newP += "/" + it
            } else if (it == "action") {
               begin = true
            }
         }

         if (newP.isEmpty()) {
            newP = p.replace(".", "/")
         }

         path = newP
      }
   }
}
