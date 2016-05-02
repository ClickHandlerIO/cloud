package io.clickhandler.action;

/**
 *
 */
public interface InternalActorAction<ACTOR, IN, OUT> extends Action<IN, OUT> {
    ACTOR getContext();
}
