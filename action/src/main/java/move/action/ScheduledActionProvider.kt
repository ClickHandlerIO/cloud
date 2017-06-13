package move.action

import io.vertx.rxjava.core.Vertx
import javax.inject.Inject
import javax.inject.Provider

/**

 */
open class ScheduledActionProvider<A : Action<Any, Any>> @Inject
constructor(vertx: Vertx,
            actionProvider: Provider<A>,
            inProvider: Provider<Any>,
            outProvider: Provider<Any>) : ActionProvider<A, Any, Any>(
        vertx, actionProvider, inProvider, outProvider
) {
    val scheduledAction: ScheduledAction = actionClass.getAnnotation(ScheduledAction::class.java)
}
