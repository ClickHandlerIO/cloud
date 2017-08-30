package move.action

import io.vertx.rxjava.core.Vertx
import javaslang.control.Try
import kotlinx.coroutines.experimental.rx1.await
import rx.Single
import java.util.function.Consumer
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
   val remoteAction: RemoteAction? = actionClass.getAnnotation(RemoteAction::class.java)

   override val annotationTimeout: Int
      get() = remoteAction?.timeout ?: 0

   override val isRemote = true

   /**
    *
    */
   val isGuarded: Boolean
      get() = remoteAction?.guarded ?: false

   lateinit var path: String

   init {
      path = remoteAction?.path!!

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
