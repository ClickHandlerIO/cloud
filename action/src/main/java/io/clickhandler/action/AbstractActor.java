package io.clickhandler.action;

import io.vertx.core.Handler;
import io.vertx.rxjava.core.Context;
import io.vertx.rxjava.core.Vertx;
import javaslang.control.Try;
import rx.Observable;
import rx.Scheduler;

import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class AbstractActor implements Actor {
    private Status status = Status.RUNNING;
    private String name;
    private Vertx vertx;
    private String key;
    private Context context;
    private Scheduler scheduler;
    private ActorManager manager;
    private List<Observable<?>> runningActions = new LinkedList<>();

    public AbstractActor() {
        this.name = getClass().getCanonicalName();
    }

    public Vertx getVertx() {
        return vertx;
    }

    void setVertx(Vertx vertx) {
        this.vertx = vertx;
    }

    public ActorManager getManager() {
        return manager;
    }

    void setManager(ActorManager manager) {
        this.manager = manager;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getKey() {
        return key;
    }

    void setKey(String key) {
        this.key = key;
    }

    @Override
    public Context getContext() {
        return context;
    }

    void setContext(Context context) {
        this.context = context;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    public void run(Handler<Void> action) {
        context.runOnContext(action);
    }

    void stop() {
        run(event -> {
            if (runningActions.isEmpty())
                finishStop();
            else if (status == Status.RUNNING)
                status = Status.STOPPING;
        });
    }

    private void maybeStop() {
        if (status != Status.STOPPING)
            return;

        if (!runningActions.isEmpty())
            return;

        finishStop();
    }

    private void finishStop() {
        Try.run(() -> manager.onStopped(this));
        Try.run(this::stopped);
    }

    <A extends Action<IN, OUT>, S extends AbstractActor, IN, OUT> Observable<OUT> invoke(
        ActorActionProvider<A, S, IN, OUT> actionProvider,
        IN request) {
        final Observable<OUT> observable = Observable.create(subscriber -> {
            if (status != Status.RUNNING) {
                Try.run(() -> subscriber.onError(new ActorUnavailableException()));
                return;
            }

            final Observable<OUT> actionObservable = actionProvider.observe(this, request)
                .subscribeOn(scheduler)
                .observeOn(scheduler);
            runningActions.add(actionObservable);
            actionObservable.subscribe(
                $ -> {
                    try {
                        // Remove as running action.
                        runningActions.remove(actionObservable);
                        maybeStop();
                    } finally {
                        if (subscriber.isUnsubscribed())
                            return;

                        try {
                            subscriber.onNext($);
                            subscriber.onCompleted();
                        } catch (Throwable e) {
                            Try.run(() -> subscriber.onError(e));
                        }
                    }
                },
                e -> {
                    try {
                        // Remove as running action.
                        runningActions.remove(actionObservable);
                        maybeStop();
                    } finally {
                        if (subscriber.isUnsubscribed())
                            return;

                        Try.run(() -> subscriber.onError(e));
                    }
                }
            );
        });
        observable.subscribeOn(scheduler).observeOn(scheduler);
        return observable;
    }

    /**
     *
     */
    protected void started() {

    }

    /**
     *
     */
    protected void stopped() {

    }
}
