package io.clickhandler.sql.db;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import rx.Observable;

import java.util.concurrent.ExecutionException;

/**
 *
 */
public interface SqlExecutor {
    /**
     * @param task
     * @param handler
     */
    void writeRunnable(SqlRunnable task, Handler<AsyncResult<Void>> handler);

    /**
     * @param task
     * @param <T>
     * @return
     */
    <T> Observable<SqlResult<T>> write(SqlCallable<T> task);

    /**
     * @param task
     * @param handler
     * @param <T>
     */
    <T> void write(SqlCallable<T> task, Handler<AsyncResult<SqlResult<T>>> handler);

    /**
     * @param task
     * @param handler
     * @param <T>
     */
    <T> void read(SqlReadCallable<T> task, Handler<AsyncResult<T>> handler);

    /**
     * @param task
     * @param <T>
     * @throws ExecutionException
     * @throws InterruptedException
     */
    <T> T readBlocking(SqlReadCallable<T> task);

    /**
     * @param task
     * @param <T>
     * @throws ExecutionException
     * @throws InterruptedException
     */
    <T> SqlResult<T> writeBlocking(SqlCallable<T> task);

    /**
     * @param task
     * @param <T>
     * @return
     */
    <T> Observable<T> read(SqlReadCallable<T> task);

    /**
     * @param task
     * @param <T>
     * @return
     */
    <T> Observable<T> readObservable(SqlReadCallable<T> task);

    /**
     * @param task
     * @param <T>
     * @return
     */
    <T> Observable<SqlResult<T>> writeObservable(SqlCallable<T> task);
}
