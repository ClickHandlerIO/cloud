package move.action;

import com.netflix.hystrix.HystrixObservable;
import io.vertx.rxjava.core.Vertx;
import rx.Observable;
import rx.Single;

import javax.inject.Provider;

/**
 * @author Clay Molocznik
 */
public abstract class Action<IN, OUT> {
    public static final ThreadLocal<ActionContext> contextLocal = new ThreadLocal<>();

    public static ActionContext currentContext() {
        return contextLocal.get();
    }

    private IN request;
    private ActionContext context;

    private Vertx vertx;
    private Provider<IN> requestProvider;
    private Provider<OUT> replyProvider;

    public boolean isFallbackEnabled() {
        return false;
    }

    public ActionContext actionContext() {
        return context;
    }

    protected Vertx vertx() {
        return vertx;
    }

    protected Provider<IN> requestProvider() {
        return requestProvider;
    }

    protected Provider<OUT> replyProvider() {
        return replyProvider;
    }

    void init(Vertx vertx, ActionContext context, Provider<IN> requestProvider, Provider<OUT> replyProvider) {
        this.vertx = vertx;
        this.context = context;
        this.requestProvider = requestProvider;
        this.replyProvider = replyProvider;
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
    Action<IN, OUT> setRequest(IN request) {
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
        return command().observe();
    }

    /**
     * @return
     */
    public Single<OUT> single() {
        return command().observe().toSingle();
    }

    /**
     * @return
     */
    public Observable<OUT> toObservable() {
        return command().toObservable();
    }

    /**
     * @return
     */
    public Single<OUT> toSingle() {
        return command().toObservable().toSingle();
    }

    /**
     * @return
     */
    abstract HystrixObservable<OUT> command();
}
