package io.clickhandler.action;

import io.vertx.rxjava.core.Context;
import io.vertx.rxjava.core.Future;
import javaslang.control.Try;
import rx.Scheduler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
public class AbstractActor<S> implements Actor {
    private final AtomicReference<Status> status = new AtomicReference<>(Status.NEW);
    private final AtomicReference<S> state = new AtomicReference<>();
    private final String name;
    private Context context;
    private Scheduler scheduler;
    private AtomicReference<String> key = new AtomicReference<>();
    private long startBegin;
    private long startEnd;
    private long stopBegin;
    private long stopEnd;
    private List<Future<Void>> startList;
    private List<Future<Void>> stopList;
    private Throwable cause;

    public AbstractActor() {
        this.name = getClass().getCanonicalName();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getKey() {
        return key.get();
    }

    public void setKey(String key) {
        this.key.compareAndSet(null, key);
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public Status getStatus() {
        return status.get();
    }

    @Override
    public void start(Future<Void> startFuture) {
        if (startFuture == null)
            return;

        if (status.compareAndSet(Status.NEW, Status.STARTING)) {
            startBegin = System.currentTimeMillis();
            finishStart(Future.<Void>future().setHandler(event -> {
                startEnd = System.currentTimeMillis();
                Try.run(() -> {
                    if (event.failed()) {
                        cause = event.cause();
                        status.set(Status.FAILED);
                        Try.run(() -> startFuture.fail(cause));
                        Try.run(() -> childStart(null));
                    } else {
                        status.set(Status.STOPPED);
                        Try.run(startFuture::complete);
                        Try.run(() -> childStart(null));
                        context.runOnContext($ -> started());
                    }
                });
            }));
        } else {
            childStart(startFuture);
        }
    }

    protected void finishStart(Future<Void> startFuture) {
        startFuture.complete();
    }

    protected void started() {

    }

    protected synchronized void childStart(Future<Void> future) {
        if (future == null) {
            empty(startList);
            return;
        }

        if (status.get() == Status.RUNNING) {
            Try.run(() -> future.complete());
        } else if (status.get() == Status.FAILED) {
            Try.run(() -> future.fail(cause));
        } else {
            if (startList == null)
                startList = new CopyOnWriteArrayList<>();
            startList.add(future);
        }
    }

    @Override
    public void stop(Future<Void> stopFuture) {
        if (stopFuture == null)
            return;

        if (!status.compareAndSet(Status.RUNNING, Status.STOPPING)) {
            childStop(stopFuture);
            return;
        }

        stopBegin = System.currentTimeMillis();
        finishStop(Future.<Void>future().setHandler(event -> {
            stopEnd = System.currentTimeMillis();
            Try.run(() -> {
                if (event.failed()) {
                    cause = event.cause();
                    status.set(Status.FAILED);
                    Try.run(() -> stopFuture.fail(cause));
                    Try.run(() -> childStop(null));
                } else {
                    status.set(Status.STOPPED);
                    Try.run(stopFuture::complete);
                    Try.run(() -> childStop(null));
                    context.runOnContext($ -> stopped());
                }
            });
        }));
    }

    protected synchronized void childStop(Future<Void> future) {
        if (future == null) {
            empty(stopList);
            return;
        }

        if (status.get() == Status.STOPPED) {
            Try.run(() -> future.complete());
        } else if (status.get() == Status.FAILED) {
            Try.run(() -> future.fail(cause));
        } else if (status.get() == Status.RUNNING) {
            Try.run(() -> future.fail(new RuntimeException("Stop Failed because it was in the process of STARTING when trying to STOP")));
        } else {
            if (stopList == null)
                stopList = new CopyOnWriteArrayList<>();
            stopList.add(future);
        }
    }

    protected void finishStop(Future<Void> stopFuture) {
        stopFuture.complete();
    }

    protected void stopped() {

    }

    private void empty(List<Future<Void>> futures) {
        if (futures != null)
            futures.forEach(this::failOrComplete);
    }

    private void failOrComplete(Future<Void> future) {
        if (future == null)
            return;

        Try.run(() -> {
            if (cause != null) {
                future.fail(cause);
            } else {
                future.complete();
            }
        });
    }
}
