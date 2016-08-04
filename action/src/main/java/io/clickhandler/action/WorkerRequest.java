package io.clickhandler.action;

import rx.Subscriber;

/**
 *
 */
public class WorkerRequest {
    public String type;
    public int delaySeconds;
    public String payload;
    Subscriber<? super Boolean> subscriber;

    public WorkerRequest type(final String type) {
        this.type = type;
        return this;
    }

    public WorkerRequest delaySeconds(final int delaySeconds) {
        this.delaySeconds = delaySeconds;
        return this;
    }

    public WorkerRequest payload(final String payload) {
        this.payload = payload;
        return this;
    }
}
