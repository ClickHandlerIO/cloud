package io.clickhandler.action;

/**
 *
 */
public abstract class AbstractBlockingScheduledAction extends AbstractBlockingAction<Void, Void> {
    @Override
    public Void handle(Void request) {
        run();
        return null;
    }

    protected abstract void run();
}
