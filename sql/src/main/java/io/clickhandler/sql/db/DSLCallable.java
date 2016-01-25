package io.clickhandler.sql.db;

import org.jooq.DSLContext;

/**
 *
 */
public interface DSLCallable<T> {
    T call(DSLContext sql);
}
