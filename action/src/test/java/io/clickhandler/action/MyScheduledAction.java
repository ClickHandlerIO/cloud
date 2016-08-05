package io.clickhandler.action;

import javax.inject.Inject;

/**
 *
 */
@ActionConfig(maxExecutionMillis = 10000, maxConcurrentRequests = 1)
@ScheduledAction(intervalSeconds = 1, type = ScheduledActionType.CLUSTER_SINGLETON)
public class MyScheduledAction extends AbstractScheduledAction {
    @Inject
    public MyScheduledAction() {
    }

    @Override
    protected void start() {
        Main.WireUp.instance.actions().io().clickhandler().action().myWorker()
            .send(new MyWorker.Request(), success -> done());
    }
}
