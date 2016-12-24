package move.sql;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import org.jooq.Condition;
import org.slf4j.Logger;
import rx.Observable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 *
 */
public interface SqlExecutor {
    /**
     *
     * @param entityClass
     * @param id
     * @param <T>
     * @return
     */
    <T extends AbstractEntity> Observable<T> get(Class<T> entityClass, String id);

    /**
     *
     * @param entityClass
     * @param ids
     * @param <T>
     * @return
     */
    <T extends AbstractEntity> Observable<List<T>> get(Class<T> entityClass, String... ids);

    /**
     *
     * @param entityClass
     * @param ids
     * @param <T>
     * @return
     */
    <T extends AbstractEntity> Observable<List<T>> get(Class<T> entityClass, Stream<String> ids);

    /**
     *
     * @param entityClass
     * @param ids
     * @param <T>
     * @return
     */
    <T extends AbstractEntity> Observable<List<T>> get(Class<T> entityClass, Collection<String> ids);

    /**
     *
     * @param cls
     * @param condition
     * @param <T>
     * @return
     */
    <T extends AbstractEntity> Observable<List<T>> select(Class<T> cls, Condition condition);

    /**
     *
     * @param cls
     * @param condition
     * @param limit
     * @param <T>
     * @return
     */
    <T extends AbstractEntity> Observable<List<T>> select(Class<T> cls, Condition condition, int limit);

    /**
     *
     * @param cls
     * @param conditions
     * @param <T>
     * @return
     */
    <T extends AbstractEntity> Observable<List<T>> select(Class<T> cls, Collection<? extends Condition> conditions);

    /**
     *
     * @param cls
     * @param conditions
     * @param limit
     * @param <T>
     * @return
     */
    <T extends AbstractEntity> Observable<List<T>> select(Class<T> cls, Collection<? extends Condition> conditions, int limit);

    /**
     *
     * @param cls
     * @param condition
     * @param limit
     * @param <T>
     * @return
     */
    <T extends AbstractEntity> Observable<Map<String, T>> selectMap(Class<T> cls, Condition condition, int limit);

    /**
     *
     * @param cls
     * @param condition
     * @param <T>
     * @return
     */
    <T> Observable<T> selectOne(Class<T> cls, Condition condition);

    <T> Observable<T> selectOne(Class<T> cls, Collection<? extends Condition> conditions);

    <T> Observable<T> selectOne(Class<T> cls, Condition... conditions);

    /**
     *
     * @param entityClass
     * @param ids
     * @param <E>
     * @return
     */
    <E extends AbstractEntity> Observable<Map<String, E>> getMap(Class<E> entityClass,
                                                                 Collection<String> ids);

    /**
     *
     * @param entityClass
     * @param ids
     * @param <E>
     * @return
     */
    <E extends AbstractEntity> Observable<Map<String, E>> getMap(Class<E> entityClass,
                                                                 Stream<String> ids);

    /**
     *
     * @param entityClass
     * @param toMap
     * @param ids
     * @param <E>
     * @return
     */
    <E extends AbstractEntity> Observable<Map<String, E>> getMap(Class<E> entityClass,
                                                                 Map<String, E> toMap,
                                                                 Collection<String> ids);

    /**
     *
     * @param batch
     * @param <T>
     * @return
     */
    <T extends AbstractEntity> Observable<SqlResult<int[]>> batch(Function<SqlBatch, SqlBatch> batch);

    <T extends AbstractEntity> Observable<SqlResult<int[]>> batch(
        Function<SqlBatch, SqlBatch> batch,
        int timeoutSeconds);

    <T extends AbstractEntity> Observable<SqlResult<int[]>> batchAtomic(Function<SqlBatch, SqlBatch> batch);

    <T extends AbstractEntity> Observable<SqlResult<int[]>> batchAtomic(
        Function<SqlBatch, SqlBatch> batch,
        int timeoutSeconds);

    /**
     *
     * @param batch
     * @param logger
     * @param <T>
     * @return
     */
    <T extends AbstractEntity> Observable<SqlResult<int[]>> batch(Function<SqlBatch, SqlBatch> batch, Logger logger);

    <T extends AbstractEntity> Observable<SqlResult<int[]>> batch(
        Function<SqlBatch, SqlBatch> batch,
        int timeoutSeconds,
        Logger logger);

    <T extends AbstractEntity> Observable<SqlResult<int[]>> batchAtomic(Function<SqlBatch, SqlBatch> batch, Logger logger);

    <T extends AbstractEntity> Observable<SqlResult<int[]>> batchAtomic(
        Function<SqlBatch, SqlBatch> batch,
        int timeoutSeconds,
        Logger logger);

    /**
     *
     * @param entity
     * @param <T>
     * @return
     */
    <T extends AbstractEntity> Observable<SqlResult<Integer>> insert(T entity);

    <T extends AbstractEntity> Observable<SqlResult<Integer>> insertAtomic(T entity);

    /**
     *
     * @param entities
     * @param <T>
     * @return
     */
    <T extends AbstractEntity> Observable<SqlResult<int[]>> insert(List<T> entities);

    <T extends AbstractEntity> Observable<SqlResult<int[]>> insert(List<T> entities, int timeoutSeconds);

    <T extends AbstractEntity> Observable<SqlResult<int[]>> insertAtomic(List<T> entities);

    <T extends AbstractEntity> Observable<SqlResult<int[]>> insertAtomic(List<T> entities, int timeoutSeconds);

    /**
     *
     * @param entity
     * @param <T>
     * @return
     */
    <T extends AbstractEntity> Observable<SqlResult<Integer>> update(T entity);

    <T extends AbstractEntity> Observable<SqlResult<Integer>> updateAtomic(T entity);

    /**
     *
     * @param entities
     * @param <T>
     * @return
     */
    <T extends AbstractEntity> Observable<SqlResult<int[]>> update(List<T> entities);

    /**
     * @param task
     * @param handler
     */
    void writeRunnable(SqlRunnable task, Handler<AsyncResult<Void>> handler);

    <T extends AbstractEntity> Observable<SqlResult<int[]>> update(List<T> entities, int timeoutSeconds);

    <T extends AbstractEntity> Observable<SqlResult<int[]>> updateAtomic(List<T> entities);

    <T extends AbstractEntity> Observable<SqlResult<int[]>> updateAtomic(List<T> entities, int timeoutSeconds);

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
