package io.clickhandler.sql.db;

/**
 *
 */
public class PersistException extends RuntimeException {
    public PersistException() {
    }

    public PersistException(String s) {
        super(s);
    }

    public PersistException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public PersistException(Throwable throwable) {
        super(throwable);
    }
}
