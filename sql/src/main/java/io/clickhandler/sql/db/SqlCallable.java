package io.clickhandler.sql.db;

/**
 *
 */
//@FunctionalInterface
public interface SqlCallable<T> {
    SqlResult<T> call(SqlSession session);
}
