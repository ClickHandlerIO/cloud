package io.clickhandler.action;

import javax.inject.Inject;

/**
 *
 */
@ActionConfig(maxExecutionMillis = 30000, maxConcurrentRequests = 1)
@ScheduledAction(delaySeconds = 300, type = ScheduledActionType.CLUSTER_SINGLETON)
public class MyScheduledAction extends AbstractScheduledAction {
    @Inject
    public MyScheduledAction() {
    }

    @Override
    protected void start(Void request) {
        respond(null);
    }
}
