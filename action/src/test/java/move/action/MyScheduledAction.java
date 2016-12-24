package move.action;

import javax.inject.Inject;

/**
 *
 */
@ActionConfig(maxExecutionMillis = 10000, maxConcurrentRequests = 1)
@ScheduledAction(intervalSeconds = 1, type = ScheduledActionType.CLUSTER_SINGLETON)
public class MyScheduledAction extends AbstractBlockingScheduledAction {
    @Inject
    public MyScheduledAction() {
    }

    @Override
    protected void run() {
        Main.WireUp.instance.actions().move().action().myWorker()
            .send(new MyWorker.Request()).toBlocking().first();
    }
}
