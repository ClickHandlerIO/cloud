package io.clickhandler.sql.db;

/**
 *
 */
public class SqlResult<T> {
    private final boolean success;
    private final T result;

    public SqlResult(boolean success, T result) {
        this.success = success;
        this.result = result;
    }

    public static <T> SqlResult<T> success() {
        return new SqlResult<>(true, null);
    }

    public static <T> SqlResult<T> success(T result) {
        return new SqlResult<>(true, result);
    }

    public static <T> SqlResult<T> rollback() {
        return new SqlResult<>(false, null);
    }

    public static <T> SqlResult<T> rollback(T result) {
        return new SqlResult<>(false, result);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isRollback() {
        return !success;
    }

    public T get() {
        return result;
    }
}
