package move.action

import javax.inject.Inject

/**
 *
 */
@ActionConfig(maxExecutionMillis = 10000, maxConcurrentRequests = 1)
@ScheduledAction(intervalSeconds = 1, type = ScheduledActionType.CLUSTER_SINGLETON)
class MyScheduledAction @Inject
constructor() : BaseScheduledAction() {
    suspend override fun execute() {
        println(javaClass.simpleName + " " + Thread.currentThread().name)
    }
}
