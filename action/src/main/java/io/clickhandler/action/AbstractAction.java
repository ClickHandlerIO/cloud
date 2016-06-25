package io.clickhandler.action;

import com.netflix.hystrix.HystrixObservable;
import rx.Observable;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Clay Molocznik
 */
public abstract class AbstractAction<IN, OUT> implements Action<IN, OUT> {
    private Object context;
    private AtomicReference<IN> request = new AtomicReference<>();

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
        return request.get();
    }

    /**
     * @param request
     * @return
     */
    public AbstractAction<IN, OUT> setRequest(IN request) {
        if (!this.request.compareAndSet(null, request)) {
            throw new IllegalStateException("setRequest has already been called");
        }
        return this;
    }

    public IN request() {
        return request.get();
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
    public Observable<OUT> toObservable() {
        return getCommand().toObservable();
    }

    /**
     * @param request
     * @param <IN>
     * @param <OUT>
     * @return
     */
//    @Deprecated
//    public <A extends Action<IN, OUT>, IN, OUT> OUT execute(
//        final IN request) {
//        try {
//            // TODO: Fix
//            return null;
//        } catch (Throwable e) {
//            throw new Error(e);
//        }
//    }

    /**
     * @return
     */
    protected abstract HystrixObservable<OUT> getCommand();
}
