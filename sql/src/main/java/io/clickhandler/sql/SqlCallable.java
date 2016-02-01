package io.clickhandler.sql;

/**
 *
 */
//@FunctionalInterface
public interface SqlCallable<T> {
    SqlResult<T> call(SqlSession session);
}
