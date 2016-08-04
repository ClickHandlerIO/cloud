package io.clickhandler.action;

import javax.inject.Inject;

/**
 *
 */
public class ScheduledActionProvider<A extends Action<Void, Void>> extends ActionProvider<A, Void, Void> {
    private static final Object DEFAULT_CONTEXT = new Object();

    private ScheduledAction scheduledAction;

    @Inject
    public ScheduledActionProvider() {
    }

    public ScheduledAction getScheduledAction() {
        return scheduledAction;
    }

    @Override
    protected void init() {
        scheduledAction = getActionClass().getAnnotation(ScheduledAction.class);
        super.init();
    }
}
