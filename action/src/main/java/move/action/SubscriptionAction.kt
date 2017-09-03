package move.action

import rx.Single
import rx.Subscription

/**
 *
 */
abstract class SubscriptionAction : Action<Unit, Unit>() {
   fun listenOn(channel: String, numberOfMessages: Int = 0): Subscription {
      return Single.just("").subscribe()
   }
}