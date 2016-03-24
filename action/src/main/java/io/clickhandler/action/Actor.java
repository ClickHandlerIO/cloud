package io.clickhandler.action;

import io.vertx.rxjava.core.Context;
import io.vertx.rxjava.core.Future;
import rx.Scheduler;

/**
 *
 */
public interface Actor {
    String getName();

    String getKey();

    Context getContext();

    void setContext(Context context);

    Scheduler getScheduler();

    void setScheduler(Scheduler scheduler);

    void setKey(String key);

    Status getStatus();

    void start(Future<Void> startFuture);

    void stop(Future<Void> stopFuture);

    enum Status {
        NEW,
        STARTING,
        RUNNING,
        FAILED,
        STOPPING,
        STOPPED,;
    }
}
