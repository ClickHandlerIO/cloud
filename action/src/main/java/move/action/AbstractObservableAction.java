package move.action;

import com.netflix.hystrix.HystrixObservableCommand;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import rx.Observable;
import rx.Single;
import rx.SingleSubscriber;

import java.util.function.Consumer;

/**
 * @author Clay Molocznik
 */
public abstract class AbstractObservableAction<IN, OUT>
    extends AbstractAction<IN, OUT>
    implements ObservableAction<IN, OUT> {

    private HystrixObservableCommand<OUT> command;
    private HystrixObservableCommand.Setter setter;

    private SingleSubscriber<? super OUT> subscriber;
    private Context ctx;
    private boolean fallback;

    public boolean isFallback() {
        return fallback;
    }

    protected HystrixObservableCommand.Setter getCommandSetter() {
        return setter;
    }

    void setCommandSetter(HystrixObservableCommand.Setter setter) {
        this.setter = setter;
    }

    /**
     * @return
     */
    protected HystrixObservableCommand<OUT> build() {
        return new HystrixObservableCommand<OUT>(getCommandSetter()) {
            @Override
            protected Observable<OUT> construct() {
                return AbstractObservableAction.this.construct();
            }

            @Override
            protected Observable<OUT> resumeWithFallback() {
                return constructFallback();
            }
        };
    }

    /**
     * @return
     */
    protected Observable<OUT> construct() {
        return Single.<OUT>create(subscriber -> {
            try {
                ctx = Vertx.currentContext();
                if (!subscriber.isUnsubscribed()) {
                    contextLocal.set(actionContext());
                    try {
                        start(subscriber);
                    } finally {
                        contextLocal.remove();
                    }
                }
            } catch (Exception e) {
                contextLocal.set(actionContext());
                try {
                    subscriber.onError(e);
                } finally {
                    contextLocal.remove();
                }
            }
        }).toObservable();
    }

    protected Observable<OUT> constructFallback() {
        return Single.<OUT>create(subscriber -> {
            try {
                ctx = Vertx.currentContext();
                if (!subscriber.isUnsubscribed()) {
                    contextLocal.set(actionContext());
                    try {
                        startFallback(subscriber);
                    } finally {
                        contextLocal.remove();
                    }
                }
            } catch (Exception e) {
                contextLocal.set(actionContext());
                try {
                    subscriber.onError(e);
                } finally {
                    contextLocal.remove();
                }
            }
        }).toObservable();
    }

    /**
     * @return
     */
    protected final HystrixObservableCommand<OUT> getCommand() {
        if (command != null) {
            return command;
        }
        command = build();
        return command;
    }

    /**
     * @param subscriber
     */
    protected void start(SingleSubscriber<? super OUT> subscriber) {
        this.subscriber = subscriber;
        start(getRequest());
    }

    /**
     * @param subscriber
     */
    protected void startFallback(SingleSubscriber<? super OUT> subscriber) {
        this.subscriber = subscriber;
        this.fallback = true;
        startFallback(getRequest());
    }

    /**
     * @param request
     */
    protected abstract void start(IN request);

    /**
     * @param request
     */
    protected void startFallback(IN request) {
        throw new ActionFallbackException();
    }

    /**
     * @param response
     */
    protected void respond(OUT response) {
        complete(response);
    }

    /**
     * @param response
     */
    protected void complete(OUT response) {
        if (subscriber != null) {
            complete(subscriber, response);
        }
    }

    /**
     * @param subscriber
     * @param response
     */
    protected void complete(SingleSubscriber<? super OUT> subscriber, OUT response) {
        try {
            if (!subscriber.isUnsubscribed()) {
                subscriber.onSuccess(response);
            }
        } catch (Exception e) {
            subscriber.onError(e);
        }
    }

    @Deprecated
    protected <A extends Action<I, O>, I, O> Observable<O> pipe(ActionProvider<A, I, O> provider,
                                                                Consumer<I> inCallback) {
        final I in = provider.getInProvider().get();
        if (inCallback != null) inCallback.accept(in);
        return observe(provider, in);
    }

    @Deprecated
    protected <A extends Action<I, O>, I, O> Observable<O> observeWithSupplier(ActionProvider<A, I, O> provider,
                                                                               Consumer<I> inCallback) {
        final I in = provider.getInProvider().get();
        if (inCallback != null) inCallback.accept(in);
        return observe(provider, in);
    }

    @Deprecated
    protected <A extends Action<I, O>, I, O> Observable<O> observe(ActionProvider<A, I, O> provider,
                                                                   I in) {
        return provider.observe(in);
    }
}
