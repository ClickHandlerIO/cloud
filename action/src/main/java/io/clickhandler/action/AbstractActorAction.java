package io.clickhandler.action;

/**
 *
 */
public abstract class AbstractActorAction<ACTOR extends AbstractActor<S>, S, IN, OUT> extends AbstractObservableAction<IN, OUT> {
    @Override
    public ACTOR getContext() {
        return (ACTOR) super.getContext();
    }

    protected ACTOR context() {
        return getContext();
    }

    public ACTOR getActor() {
        return getContext();
    }

    protected ACTOR actor() {
        return getContext();
    }

    protected S state() {
        return getContext().state();
    }
}
