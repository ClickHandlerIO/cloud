package io.clickhandler.sql.db;

/**
 *
 */
public interface DatabaseSessionCallable<T> {
    T call(DatabaseSession session);
}
