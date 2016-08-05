package io.clickhandler.action;

/**
 *
 */
public abstract class AbstractScheduledAction extends AbstractObservableAction<Void, Void> {
    public AbstractScheduledAction() {
    }

    @Override
    protected void start(Void request) {
        start();
    }

    protected abstract void start();
}
