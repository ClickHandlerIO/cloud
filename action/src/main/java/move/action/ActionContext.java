package move.action;

import io.vertx.core.Context;

/**
 *
 */
public class ActionContext {
    public final long started = System.currentTimeMillis();
    public final long timesOutAt;
    public final ActionProvider entry;
    public final Context context;

    public ActionContext(long timeoutMillis, ActionProvider entry, Context context) {
        this.timesOutAt = started + timeoutMillis;
        this.entry = entry;
        this.context = context;
    }
}
