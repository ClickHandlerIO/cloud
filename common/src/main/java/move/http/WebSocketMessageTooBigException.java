package move.http;

/**
 *
 */
public class WebSocketMessageTooBigException extends RuntimeException {
    public final int size;

    public WebSocketMessageTooBigException(int size) {
        this.size = size;
    }

    public WebSocketMessageTooBigException(String message, int size) {
        super(message);
        this.size = size;
    }

    public WebSocketMessageTooBigException(String message, Throwable cause, int size) {
        super(message, cause);
        this.size = size;
    }

    public WebSocketMessageTooBigException(Throwable cause, int size) {
        super(cause);
        this.size = size;
    }

    public WebSocketMessageTooBigException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, int size) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.size = size;
    }
}
