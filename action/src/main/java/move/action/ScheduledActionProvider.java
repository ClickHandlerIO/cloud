package move.action;

import javax.inject.Inject;

/**
 *
 */
public class ScheduledActionProvider<A extends Action<Object, Object>> extends ActionProvider<A, Object, Object> {
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
