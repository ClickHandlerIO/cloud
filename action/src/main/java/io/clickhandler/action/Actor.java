package io.clickhandler.action;

import io.vertx.rxjava.core.Context;
import io.vertx.rxjava.core.Vertx;
import rx.Scheduler;

/**
 *
 */
public interface Actor {
    Vertx getVertx();

    String getName();

    String getKey();

    Context getContext();

    Scheduler getScheduler();

    Status getStatus();

    enum Status {
        RUNNING,
        STOPPING,
        STOPPED;
    }
}
