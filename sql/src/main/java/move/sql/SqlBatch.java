package move.sql;

import org.jooq.*;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * Bundles multiple SQL write queries into a single batch call.
 */
public class SqlBatch {
    public final SqlSession session;
    private final List<Query> queryList = new ArrayList<>();

    SqlBatch(SqlSession session) {
        this.session = session;
    }

    /**
     *
     * @return
     */
    public SqlSession getSession() {
        return session;
    }

    /**
     *
     * @return
     */
    public DSLContext create() {
        return session.create();
    }

    /**
     *
     * @param entity
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity> SqlBatch insert(E entity) {
        if (entity == null)
            return this;

        queryList.add(session.insertQuery(entity));
        return this;
    }

    /**
     *
     * @param stream
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity> SqlBatch insert(Stream<? extends AbstractEntity> stream) {
        if (stream == null)
            return this;

        stream.forEach(v -> queryList.add(session.insertQuery(v)));
        return this;
    }

    /**
     *
     * @param entities
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity> SqlBatch insert(Collection<? extends AbstractEntity> entities) {
        if (entities == null || entities.isEmpty())
            return this;

        entities.forEach(entity -> queryList.add(session.insertQuery(entity)));
        return this;
    }

    /**
     *
     * @param query
     * @return
     */
    public SqlBatch update(Update query) {
        if (query == null)
            return this;

        queryList.add(query);
        return this;
    }

    /**
     *
     * @param entity
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity> SqlBatch update(E entity) {
        if (entity == null)
            return this;

        queryList.add(session.updateQuery(entity));
        return this;
    }

    /**
     *
     * @param entity
     * @param condition
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity> SqlBatch update(E entity, Condition condition) {
        if (entity == null)
            return this;

        queryList.add(session.updateQuery(entity, condition));
        return this;
    }

    /**
     *
     * @param stream
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity> SqlBatch update(Stream<? extends Update> stream) {
        if (stream == null)
            return this;

        stream.forEach(queryList::add);
        return this;
    }

    /**
     *
     * @param stream
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity> SqlBatch updateStream(Stream<? extends AbstractEntity> stream) {
        if (stream == null)
            return this;

        stream.forEach(entity -> queryList.add(session.updateQuery(entity)));
        return this;
    }

    /**
     *
     * @param entities
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity> SqlBatch update(Collection<? extends AbstractEntity> entities) {
        if (entities == null || entities.isEmpty())
            return this;

        entities.forEach(entity -> queryList.add(session.updateQuery(entity)));
        return this;
    }

    /**
     *
     * @param entity
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity> SqlBatch delete(E entity) {
        if (entity == null)
            return this;

        queryList.add(session.deleteQuery(entity));
        return this;
    }

    /**
     *
     * @param entity
     * @param condition
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity> SqlBatch delete(E entity, Condition condition) {
        if (entity == null)
            return this;

        queryList.add(session.deleteQuery(entity, condition));
        return this;
    }

    /**
     *
     * @param entities
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity> SqlBatch delete(Collection<? extends AbstractEntity> entities) {
        if (entities == null || entities.isEmpty())
            return this;

        entities.forEach(entity -> queryList.add(session.deleteQuery(entity)));
        return this;
    }

    /**
     *
     * @return
     */
    public SqlResult<int[]> execute() {
        if (queryList.isEmpty())
            return SqlResult.commit(new int[0]);

        return session.save(queryList);
    }

    /**
     *
     * @param timeoutSeconds
     * @return
     */
    public SqlResult<int[]> execute(int timeoutSeconds) {
        if (queryList.isEmpty())
            return SqlResult.commit(new int[0]);

        return session.save(queryList, timeoutSeconds);
    }

    /**
     *
     * @param logger
     * @return
     */
    public SqlResult<int[]> execute(Logger logger) {
        if (queryList.isEmpty())
            return SqlResult.commit(new int[0]);

        return session.save(queryList, logger);
    }

    /**
     *
     * @param timeoutSeconds
     * @param logger
     * @return
     */
    public SqlResult<int[]> execute(int timeoutSeconds, Logger logger) {
        if (queryList.isEmpty())
            return SqlResult.commit(new int[0]);

        return session.save(queryList, timeoutSeconds, logger);
    }

    /**
     *
     * @return
     */
    public SqlResult<int[]> executeAtomic() {
        if (queryList.isEmpty())
            return SqlResult.commit(new int[0]);

        return session.saveAtomic(queryList);
    }

    /**
     *
     * @param timeoutSeconds
     * @return
     */
    public SqlResult<int[]> executeAtomic(int timeoutSeconds) {
        if (queryList.isEmpty())
            return SqlResult.commit(new int[0]);

        return session.saveAtomic(queryList, timeoutSeconds);
    }

    /**
     *
     * @param logger
     * @return
     */
    public SqlResult<int[]> executeAtomic(Logger logger) {
        if (queryList.isEmpty())
            return SqlResult.commit(new int[0]);

        return session.saveAtomic(queryList, logger);
    }

    /**
     *
     * @param timeoutSeconds
     * @param logger
     * @return
     */
    public SqlResult<int[]> executeAtomic(int timeoutSeconds, Logger logger) {
        if (queryList.isEmpty())
            return SqlResult.commit(new int[0]);

        return session.saveAtomic(queryList, timeoutSeconds, logger);
    }
}
