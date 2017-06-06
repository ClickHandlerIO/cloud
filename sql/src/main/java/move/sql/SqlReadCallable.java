package move.sql;

/**
 *
 */
@FunctionalInterface
public interface SqlReadCallable<T> {
    T call(SqlSession session);
}
