package move.action

import io.vertx.rxjava.core.Vertx
import javax.inject.Inject
import javax.inject.Provider

/**

 */
open class InternalActionProvider<A : Action<IN, OUT>, IN : Any, OUT : Any> @Inject
constructor(vertx: Vertx,
            actionProvider: Provider<A>) : ActionProvider<A, IN, OUT>(
   vertx, actionProvider
) {
   override val isInternal = true

   val annotation: Internal? = actionClass.getAnnotation(Internal::class.java)

   override val annotationTimeout: Int
      get() = annotation?.timeout ?: 0

   override fun findName(): String {
      if (annotation?.value?.isNotBlank() == true) {
         return annotation.value.trim()
      }
      return super.findName()
   }
}
