package move.action;

import com.netflix.hystrix.HystrixObservable;
import io.vertx.rxjava.core.Vertx;
import rx.Observable;
import rx.Single;

/**
 * @author Clay Molocznik
 */
public abstract class AbstractAction<IN, OUT> implements Action<IN, OUT> {
    public static final ThreadLocal<ActionContext> contextLocal = new ThreadLocal<>();

    private IN request;
    private ActionContext context;
    private boolean forceAsync;

    public boolean isFallbackEnabled() {
        return false;
    }

    public ActionContext actionContext() {
        return context;
    }

    void setContext(ActionContext context) {
        this.context = context;
    }

    public boolean isForceAsync() {
        return forceAsync;
    }

    void setForceAsync(boolean forceAsync) {
        this.forceAsync = forceAsync;
    }

    public Vertx vertx() {
        if (context == null) {
            return null;
        } else {
            return context.entry.vertx;
        }
    }

    /**
     * @return
     */
    public IN getRequest() {
        return request;
    }

    /**
     * @param request
     * @return
     */
    public AbstractAction<IN, OUT> setRequest(IN request) {
        this.request = request;
        return this;
    }

    /**
     * @return
     */
    public IN request() {
        return request;
    }

    /**
     * @return
     */
    public Observable<OUT> observe() {
        return getCommand().observe();
    }

    /**
     * @return
     */
    public Single<OUT> single() {
        return getCommand().observe().toSingle();
    }

    /**
     * @return
     */
    public Observable<OUT> toObservable() {
        return getCommand().toObservable();
    }

    /**
     * @return
     */
    public Single<OUT> toSingle() {
        return getCommand().toObservable().toSingle();
    }

    /**
     * @return
     */
    protected abstract HystrixObservable<OUT> getCommand();
}
