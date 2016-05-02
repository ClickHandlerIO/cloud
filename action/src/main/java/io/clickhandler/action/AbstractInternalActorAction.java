package io.clickhandler.action;

/**
 *
 */
public abstract class AbstractInternalActorAction<ACTOR, IN, OUT> extends AbstractObservableAction<IN, OUT> implements InternalActorAction<ACTOR, IN, OUT> {
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
}
