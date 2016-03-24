package io.clickhandler.action;

/**
 *
 */
public abstract class AbstractActorAction<S, IN, OUT> extends AbstractObservableAction<IN, OUT> {
    @Override
    public S getContext() {
        return (S) super.getContext();
    }

    public S getActor() {
        return getContext();
    }
}
