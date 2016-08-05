package io.clickhandler.action;

import rx.Subscriber;

/**
 *
 */
public class WorkerRequest {
    public WorkerActionProvider actionProvider;
    public int delaySeconds;
    public Object payload;
    Subscriber<? super Boolean> subscriber;

    public WorkerRequest actionProvider(final WorkerActionProvider actionProvider) {
        this.actionProvider = actionProvider;
        return this;
    }

    public WorkerRequest delaySeconds(final int delaySeconds) {
        this.delaySeconds = delaySeconds;
        return this;
    }

    public WorkerRequest payload(final Object payload) {
        this.payload = payload;
        return this;
    }
}
