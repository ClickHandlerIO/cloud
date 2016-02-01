package io.clickhandler.sql;

/**
 *
 */
public interface SqlReadCallable<T> {
    T call(SqlSession session);
}
