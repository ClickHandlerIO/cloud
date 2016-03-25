package io.clickhandler.action;

/**
 *
 */
public abstract class AbstractActorAction<S, IN, OUT> extends AbstractObservableAction<IN, OUT> {
    @Override
    public S getContext() {
        return (S) super.getContext();
    }

    protected S context() {
        return getContext();
    }

    public S getActor() {
        return getContext();
    }

    protected S actor() {
        return getContext();
    }
}
