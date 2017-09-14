package move;

/**
 *
 */
public class WireException extends RuntimeException {

  public WireException() {
  }

  public WireException(String message) {
    super(message);
  }

  public WireException(String message, Throwable cause) {
    super(message, cause);
  }

  public WireException(Throwable cause) {
    super(cause);
  }

  public WireException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
