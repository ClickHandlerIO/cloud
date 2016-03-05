package io.clickhandler.sql;

/**
 *
 */
public class SqlResult<T> {
    private final boolean success;
    private final T result;
    private final Throwable reason;

    public SqlResult(boolean success, T result) {
        this(success, result, null);
    }

    public SqlResult(boolean success, T result, Throwable reason) {
        this.success = success;
        this.result = result;
        this.reason = reason;
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

    public static <T> SqlResult<T> rollback(T result, Throwable reason) {
        return new SqlResult<>(false, result, reason);
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

    public Throwable getReason() {
        return reason;
    }
}
