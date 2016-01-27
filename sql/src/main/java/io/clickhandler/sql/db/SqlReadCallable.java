package io.clickhandler.sql.db;

/**
 *
 */
public interface SqlReadCallable<T> {
    T call(SqlSession session);
}
