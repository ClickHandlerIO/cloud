package move.action

import kotlinx.coroutines.experimental.rx1.await
import rx.Single

/**
 *
 */
abstract class InternalAction<IN : Any, OUT : Any> : Action<IN, OUT>() {

   /**
    *
    */
   override public fun rx(request: IN): Single<OUT> {
      if (_request != null) {
         // Create a new instance.
         return provider.create().rx(request)
      }

      _request = request
      return single
   }

   /**
    *
    */
   override public suspend operator fun invoke(request: IN): OUT {
      if (_request != null) {
         return provider.create().invoke(request)
      }

      _request = request
      return single.await()
   }

   /**
    *
    */
   override public suspend infix fun await(request: IN): OUT {
      if (_request != null) {
         return provider.create().await(request)
      }

      _request = request
      return single.await()
   }
}
