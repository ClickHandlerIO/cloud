package move.action

import io.vertx.rxjava.core.Vertx
import javax.inject.Inject
import javax.inject.Provider

/**

 */
open class ScheduledActionProvider<A : Action<Unit, Unit>> @Inject
constructor(vertx: Vertx,
            actionProvider: Provider<A>)
    : ActionProvider<A, Unit, Unit>(
        vertx,
        actionProvider,
        Provider<Unit> { },
        Provider<Unit> { }
) {
    override val isScheduled = true

    val scheduledAction: ScheduledAction = actionClass.getAnnotation(ScheduledAction::class.java)
}
