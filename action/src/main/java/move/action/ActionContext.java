package move.action;

import io.vertx.rxjava.ext.web.RoutingContext;

/**
 *
 */
public class ActionContext {

  public final long started;
  public final long timesOutAt;
  public final ActionProvider entry;
  public final ActionEventLoopContext eventLoop;
  public Object data;
  volatile long currentTimeout;
  RoutingContext webRequest;

  public ActionContext(
      long timesOutAt,
      ActionProvider entry,
      ActionEventLoopContext eventLoop,
      Object data) {
    this(System.currentTimeMillis(), timesOutAt, entry, eventLoop);
    this.data = data;
  }

  public ActionContext(
      long started,
      long timeoutMillis,
      ActionProvider entry,
      ActionEventLoopContext eventLoop) {
    this.started = started;
    if (timeoutMillis > 0L) {
      this.currentTimeout = this.timesOutAt = started + timeoutMillis;
    } else {
      this.currentTimeout = this.timesOutAt = 0L;
    }
    this.entry = entry;
    this.eventLoop = eventLoop;
  }

  public <T> T data() {
    return (T) data;
  }

  @Override
  public String toString() {
    return "ActionContext{" +
        "started=" + started +
        ", timesOutAt=" + timesOutAt +
        ", entry=" + entry + "[" + entry.getActionClass().getCanonicalName() + "]" +
        ", eventLoop=" + eventLoop +
        ", data=" + data +
        '}';
  }
}
