package io.clickhandler.sql;

import org.jooq.*;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 *
 */
public class SqlBatch {
    public final SqlSession session;
    private final List<Query> queryList = new ArrayList<>();

    SqlBatch(SqlSession session) {
        this.session = session;
    }

    public SqlSession getSession() {
        return session;
    }

    public DSLContext create() {
        return session.create();
    }

    public <E extends AbstractEntity> SqlBatch insert(E entity) {
        if (entity == null)
            return this;

        queryList.add(session.insertQuery(entity));
        return this;
    }

    public <E extends AbstractEntity> SqlBatch insert(Stream<? extends AbstractEntity> stream) {
        if (stream == null)
            return this;

        stream.forEach(v -> queryList.add(session.insertQuery(v)));
        return this;
    }

    public <E extends AbstractEntity> SqlBatch insert(Collection<? extends AbstractEntity> entities) {
        if (entities == null || entities.isEmpty())
            return this;

        entities.forEach(entity -> queryList.add(session.insertQuery(entity)));
        return this;
    }

    public <E extends AbstractEntity> SqlBatch update(E entity) {
        if (entity == null)
            return this;

        queryList.add(session.updateQuery(entity));
        return this;
    }

    public <E extends AbstractEntity> SqlBatch update(E entity, Condition condition) {
        if (entity == null)
            return this;

        queryList.add(session.updateQuery(entity, condition));
        return this;
    }

    public <E extends AbstractEntity> SqlBatch update(Stream<? extends Update> stream) {
        if (stream == null)
            return this;

        stream.forEach(queryList::add);
        return this;
    }

    public <E extends AbstractEntity> SqlBatch updateStream(Stream<? extends AbstractEntity> stream) {
        if (stream == null)
            return this;

        stream.forEach(entity -> queryList.add(session.updateQuery(entity)));
        return this;
    }

    public <E extends AbstractEntity> SqlBatch update(Collection<? extends AbstractEntity> entities) {
        if (entities == null || entities.isEmpty())
            return this;

        entities.forEach(entity -> queryList.add(session.updateQuery(entity)));
        return this;
    }

    public <E extends AbstractEntity> SqlBatch delete(E entity) {
        if (entity == null)
            return this;

        queryList.add(session.deleteQuery(entity));
        return this;
    }

    public <E extends AbstractEntity> SqlBatch delete(E entity, Condition condition) {
        if (entity == null)
            return this;

        queryList.add(session.deleteQuery(entity, condition));
        return this;
    }

    public <E extends AbstractEntity> SqlBatch delete(Collection<? extends AbstractEntity> entities) {
        if (entities == null || entities.isEmpty())
            return this;

        entities.forEach(entity -> queryList.add(session.deleteQuery(entity)));
        return this;
    }

    public SqlResult<int[]> execute() {
        if (queryList.isEmpty())
            return SqlResult.success(new int[0]);

        return session.save(queryList);
    }

    public SqlResult<int[]> execute(Logger logger) {
        if (queryList.isEmpty())
            return SqlResult.success(new int[0]);

        return session.save(queryList, logger);
    }
}
