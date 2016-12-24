package move.sql;

/**
 *
 */
public interface SqlReadCallable<T> {
    T call(SqlSession session);
}
