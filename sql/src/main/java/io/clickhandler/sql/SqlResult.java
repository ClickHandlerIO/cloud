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

    public static <T> SqlResult<T> commit() {
        return new SqlResult<T>(true, null);
    }

    @Deprecated
    public static <T> SqlResult<T> success() {
        return new SqlResult<>(true, null);
    }

    public static <T> SqlResult<T> commit(T result) {
        return new SqlResult<>(true, result);
    }

    @Deprecated
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

    public static SqlResult<Integer> atomic(int result) {
        if (result != 1) {
            return rollback(result);
        } else {
            return commit(result);
        }
    }

    public static SqlResult<int[]> atomic(int[] results) {
        if (results == null || results.length == 0)
            return rollback(results);

        for (int i = 0; i < results.length; i++) {
            final int result = results[i];

            if (result != 1) {
                return rollback(results);
            }
        }

        return commit(results);
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
