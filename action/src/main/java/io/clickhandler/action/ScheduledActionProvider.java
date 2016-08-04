package io.clickhandler.action;

import javax.inject.Inject;

/**
 *
 */
public class ScheduledActionProvider<A extends Action<IN, Boolean>, IN> extends ActionProvider<A, IN, Boolean> {
    private static final Object DEFAULT_CONTEXT = new Object();

    private WorkerAction workerAction;

    @Inject
    public ScheduledActionProvider() {
    }

    @Override
    protected void init() {
        workerAction = getActionClass().getAnnotation(WorkerAction.class);
        super.init();
    }
}
