package move.sql;

/**
 *
 */
//@FunctionalInterface
public interface SqlCallable<T> {

  SqlResult<T> call(SqlSession session);
}
