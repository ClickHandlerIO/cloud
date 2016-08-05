package io.clickhandler.action;

import javax.inject.Inject;

/**
 *
 */
@ActionConfig(maxExecutionMillis = 30000, maxConcurrentRequests = 1)
@ScheduledAction(intervalSeconds = 1, type = ScheduledActionType.CLUSTER_SINGLETON)
public class MyScheduledAction extends AbstractScheduledAction {
    @Inject
    public MyScheduledAction() {
    }

    @Override
    protected void start() {
        System.out.println(System.currentTimeMillis());
        respond(null);
    }
}
