package io.clickhandler.action;

/**
 *
 */
public abstract class AbstractWorkerAction<T> extends AbstractObservableAction<T, Boolean> {
    public AbstractWorkerAction() {
    }

    protected void notProcessed() {
        respond(false);
    }

    protected void processed() {
        respond(true);
    }
}
