package move.action;

import io.vertx.rxjava.core.Context;
import rx.Subscriber;

/**
 *
 */
public class WorkerRequest {
    public WorkerActionProvider actionProvider;
    public Object request;
    Subscriber<? super Boolean> subscriber;
    Context ctx;

    public WorkerRequest actionProvider(final WorkerActionProvider actionProvider) {
        this.actionProvider = actionProvider;
        return this;
    }

    public WorkerRequest request(final Object request) {
        this.request = request;
        return this;
    }
}
