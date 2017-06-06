package move.action

import javax.inject.Inject

/**

 */
@ActionConfig(maxExecutionMillis = 10000, maxConcurrentRequests = 1)
@ScheduledAction(intervalSeconds = 1000, type = ScheduledActionType.CLUSTER_SINGLETON)
class MyScheduledAction2 @Inject
constructor() : AbstractBlockingScheduledAction() {

    override fun run() {
        //        Main.WireUp.instance.actions().move().action().myWorker()
        //            .send(new MyWorker.Request()).toBlocking().first();
    }
}
