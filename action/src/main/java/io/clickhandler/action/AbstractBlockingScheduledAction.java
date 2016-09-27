package io.clickhandler.action;

/**
 *
 */
public abstract class AbstractBlockingScheduledAction extends AbstractBlockingAction<Object, Object> {
    @Override
    public Object handle(Object request) {
        run();
        return null;
    }

    protected abstract void run();
}
