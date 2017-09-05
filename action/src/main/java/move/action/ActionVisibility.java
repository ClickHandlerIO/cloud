package move.action;

/**
 *
 */
public enum ActionVisibility {
  /**
   * HTTP and WebSocket API is available.
   */
  PUBLIC,
  /**
   * Internal API is available.
   */
  INTERNAL,
  /**
   * API generation is ignored.
   */
  PRIVATE,
}
