package move.action

import javax.inject.Inject

/**

 */
@ActionConfig(maxExecutionMillis = 10000, maxConcurrentRequests = 1)
@ScheduledAction(intervalSeconds = 1000, type = ScheduledActionType.CLUSTER_SINGLETON)
class MyScheduledAction @Inject
constructor() : BaseScheduledAction() {

    suspend override fun recover(caught: Throwable, cause: Throwable, isFallback: Boolean): Any {
        throw cause
    }

    suspend override fun execute() {

    }
}
