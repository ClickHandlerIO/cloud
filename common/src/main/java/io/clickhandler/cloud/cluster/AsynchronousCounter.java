package io.clickhandler.cloud.cluster;

import io.vertx.core.*;
import io.vertx.core.shareddata.Counter;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class AsynchronousCounter implements Counter {

    private final Vertx vertx;
    private final AtomicLong counter;

    public AsynchronousCounter(Vertx vertx) {
        this.vertx = vertx;
        this.counter = new AtomicLong();
    }

    public AsynchronousCounter(Vertx vertx, AtomicLong counter) {
        this.vertx = vertx;
        this.counter = counter;
    }

    @Override
    public void get(Handler<AsyncResult<Long>> resultHandler) {
        Objects.requireNonNull(resultHandler, "resultHandler");
        Context context = vertx.getOrCreateContext();
        context.runOnContext(v -> resultHandler.handle(Future.succeededFuture(counter.get())));
    }

    @Override
    public void incrementAndGet(Handler<AsyncResult<Long>> resultHandler) {
        Objects.requireNonNull(resultHandler, "resultHandler");
        Context context = vertx.getOrCreateContext();
        context.runOnContext(v -> resultHandler.handle(Future.succeededFuture(counter.incrementAndGet())));
    }

    @Override
    public void getAndIncrement(Handler<AsyncResult<Long>> resultHandler) {
        Objects.requireNonNull(resultHandler, "resultHandler");
        Context context = vertx.getOrCreateContext();
        context.runOnContext(v -> resultHandler.handle(Future.succeededFuture(counter.getAndIncrement())));
    }

    @Override
    public void decrementAndGet(Handler<AsyncResult<Long>> resultHandler) {
        Objects.requireNonNull(resultHandler, "resultHandler");
        Context context = vertx.getOrCreateContext();
        context.runOnContext(v -> resultHandler.handle(Future.succeededFuture(counter.decrementAndGet())));
    }

    @Override
    public void addAndGet(long value, Handler<AsyncResult<Long>> resultHandler) {
        Objects.requireNonNull(resultHandler, "resultHandler");
        Context context = vertx.getOrCreateContext();
        context.runOnContext(v -> resultHandler.handle(Future.succeededFuture(counter.addAndGet(value))));
    }

    @Override
    public void getAndAdd(long value, Handler<AsyncResult<Long>> resultHandler) {
        Objects.requireNonNull(resultHandler, "resultHandler");
        Context context = vertx.getOrCreateContext();
        context.runOnContext(v -> resultHandler.handle(Future.succeededFuture(counter.getAndAdd(value))));
    }

    @Override
    public void compareAndSet(long expected, long value, Handler<AsyncResult<Boolean>> resultHandler) {
        Objects.requireNonNull(resultHandler, "resultHandler");
        Context context = vertx.getOrCreateContext();
        context.runOnContext(v -> resultHandler.handle(Future.succeededFuture(counter.compareAndSet(expected, value))));
    }
}
