package move.action;

import com.netflix.hystrix.HystrixObservable;
import rx.Observable;
import rx.Single;

/**
 * @author Clay Molocznik
 */
public abstract class AbstractAction<IN, OUT> implements Action<IN, OUT> {
    public static final ThreadLocal<ActionContext> contextLocal = new ThreadLocal<>();

    private Object context;
    private IN request;
    private ActionContext actionContext;

    public ActionContext getActionContext() {
        return actionContext;
    }

    void setActionContext(ActionContext actionContext) {
        this.actionContext = actionContext;
    }

    public Object getContext() {
        return context;
    }

    public void setContext(Object context) {
        this.context = context;
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
     *
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
     *
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
