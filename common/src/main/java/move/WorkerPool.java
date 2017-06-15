package move;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.WorkerExecutor;
import rx.Single;

/**
 *
 */
public abstract class WorkerPool {
    public static WorkerPool create(Vertx vertx, String name, int size, long timeoutNanos) {
        if (name == null || name.trim().isEmpty()) {
            return new Global(vertx);
        }

        return new Shared(vertx.createSharedWorkerExecutor(name, size, timeoutNanos));
    }

    public static WorkerPool global(Vertx vertx) {
        return new Global(vertx);
    }

    /**
     * Safely execute some blocking code.
     * <p>
     * Executes the blocking code in the handler <code>blockingCodeHandler</code> using a thread from the worker pool.
     * <p>
     * When the code is complete the handler <code>resultHandler</code> will be called with the result on the original context
     * (i.e. on the original event loop of the caller).
     * <p>
     * A <code>Future</code> instance is passed into <code>blockingCodeHandler</code>. When the blocking code successfully completes,
     * the handler should call the {@link io.vertx.rxjava.core.Future#complete} or {@link io.vertx.rxjava.core.Future#complete} method, or the {@link io.vertx.rxjava.core.Future#fail}
     * method if it failed.
     * <p>
     * In the <code>blockingCodeHandler</code> the current context remains the original context and therefore any task
     * scheduled in the <code>blockingCodeHandler</code> will be executed on the this context and not on the worker thread.
     *
     * @param blockingCodeHandler handler representing the blocking code to run
     * @param ordered             if true then if executeBlocking is called several times on the same context, the executions for that context will be executed serially, not in parallel. if false then they will be no ordering guarantees
     * @param resultHandler       handler that will be called when the blocking code is complete
     */
    public abstract <T> void executeBlocking(Handler<Future<T>> blockingCodeHandler, boolean ordered, Handler<AsyncResult<T>> resultHandler);

    /**
     * Safely execute some blocking code.
     * <p>
     * Executes the blocking code in the handler <code>blockingCodeHandler</code> using a thread from the worker pool.
     * <p>
     * When the code is complete the handler <code>resultHandler</code> will be called with the result on the original context
     * (i.e. on the original event loop of the caller).
     * <p>
     * A <code>Future</code> instance is passed into <code>blockingCodeHandler</code>. When the blocking code successfully completes,
     * the handler should call the {@link io.vertx.rxjava.core.Future#complete} or {@link io.vertx.rxjava.core.Future#complete} method, or the {@link io.vertx.rxjava.core.Future#fail}
     * method if it failed.
     * <p>
     * In the <code>blockingCodeHandler</code> the current context remains the original context and therefore any task
     * scheduled in the <code>blockingCodeHandler</code> will be executed on the this context and not on the worker thread.
     *
     * @param blockingCodeHandler handler representing the blocking code to run
     * @param ordered             if true then if executeBlocking is called several times on the same context, the executions for that context will be executed serially, not in parallel. if false then they will be no ordering guarantees
     * @return
     */
    public abstract <T> Single<T> rxExecuteBlocking(Handler<io.vertx.rxjava.core.Future<T>> blockingCodeHandler, boolean ordered);

    /**
     * Like {@link io.vertx.rxjava.core.WorkerExecutor#executeBlocking} called with ordered = true.
     *
     * @param blockingCodeHandler
     * @param resultHandler
     */
    public abstract <T> void executeBlocking(Handler<io.vertx.rxjava.core.Future<T>> blockingCodeHandler, Handler<AsyncResult<T>> resultHandler);

    /**
     * Like {@link io.vertx.rxjava.core.WorkerExecutor#executeBlocking} called with ordered = true.
     *
     * @param blockingCodeHandler
     * @return
     */
    public abstract <T> Single<T> rxExecuteBlocking(Handler<io.vertx.rxjava.core.Future<T>> blockingCodeHandler);

    /**
     * Close the executor.
     */
    public abstract void close();

    private static class Global extends WorkerPool {
        public final Vertx vertx;

        public Global(Vertx vertx) {
            this.vertx = vertx;
        }

        @Override
        public <T> void executeBlocking(Handler<Future<T>> blockingCodeHandler, boolean ordered, Handler<AsyncResult<T>> asyncResultHandler) {
            vertx.executeBlocking(blockingCodeHandler, ordered, asyncResultHandler);
        }

        @Override
        public <T> Single<T> rxExecuteBlocking(Handler<Future<T>> blockingCodeHandler, boolean ordered) {
            return vertx.rxExecuteBlocking(blockingCodeHandler, ordered);
        }

        @Override
        public <T> void executeBlocking(Handler<Future<T>> blockingCodeHandler, Handler<AsyncResult<T>> asyncResultHandler) {
            vertx.executeBlocking(blockingCodeHandler, asyncResultHandler);
        }

        @Override
        public <T> Single<T> rxExecuteBlocking(Handler<Future<T>> blockingCodeHandler) {
            return vertx.rxExecuteBlocking(blockingCodeHandler);
        }

        @Override
        public void close() {
        }
    }

    private static class Shared extends WorkerPool {
        public final WorkerExecutor executor;

        public Shared(WorkerExecutor executor) {
            this.executor = executor;
        }

        @Override
        public <T> void executeBlocking(Handler<Future<T>> blockingCodeHandler, boolean ordered, Handler<AsyncResult<T>> asyncResultHandler) {
            executor.executeBlocking(blockingCodeHandler, ordered, asyncResultHandler);
        }

        @Override
        public <T> Single<T> rxExecuteBlocking(Handler<Future<T>> blockingCodeHandler, boolean ordered) {
            return executor.rxExecuteBlocking(blockingCodeHandler, ordered);
        }

        @Override
        public <T> void executeBlocking(Handler<Future<T>> blockingCodeHandler, Handler<AsyncResult<T>> asyncResultHandler) {
            executor.executeBlocking(blockingCodeHandler, asyncResultHandler);
        }

        @Override
        public <T> Single<T> rxExecuteBlocking(Handler<Future<T>> blockingCodeHandler) {
            return executor.rxExecuteBlocking(blockingCodeHandler);
        }

        @Override
        public void close() {
            executor.close();
        }
    }
}
