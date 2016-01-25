package io.clickhandler.sql.db;

import io.clickhandler.sql.entity.AbstractEntity;
import org.jooq.Record;

/**
 * A <code>EntityMapper</code> is a mapper that can receive {@link AbstractEntity}
 * objects, when fetching data from the database, transforming them into a
 * Record type <code>&lt;E&gt;</code>
 *
 * @author Clay Molocznik
 */
public interface EntityMapper<E extends AbstractEntity, R extends Record> {
    /**
     * A callback method indicating that the next record has been fetched.
     *
     * @param entity The entity to be mapped. This is never null.
     */
    R map(E entity);

    /**
     * Merge the values of a record into an instance of an entity.
     *
     * @param record
     * @param intoEntity
     */
    void merge(R record, E intoEntity);
}
