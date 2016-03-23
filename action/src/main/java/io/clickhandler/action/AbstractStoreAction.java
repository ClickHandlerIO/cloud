package io.clickhandler.action;

/**
 *
 */
public abstract class AbstractStoreAction<S, IN, OUT> extends AbstractObservableAction<IN, OUT> {
    @Override
    public S getContext() {
        return (S) super.getContext();
    }

    public S getStore() {
        return getContext();
    }
}
