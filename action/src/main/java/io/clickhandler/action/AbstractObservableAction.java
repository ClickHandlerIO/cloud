package io.clickhandler.action;

import com.netflix.hystrix.HystrixObservableCommand;
import io.clickhandler.common.Func;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import rx.Observable;
import rx.Subscriber;

/**
 * @author Clay Molocznik
 */
public abstract class AbstractObservableAction<IN, OUT>
    extends AbstractAction<IN, OUT>
    implements ObservableAction<IN, OUT> {

    private HystrixObservableCommand<OUT> command;
    private HystrixObservableCommand.Setter setter;

    private Subscriber<? super OUT> subscriber;
    private Context ctx;

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
        };
    }

    /**
     * @return
     */
    protected Observable<OUT> construct() {
        return Observable.create(new Observable.OnSubscribe<OUT>() {
            @Override
            public void call(Subscriber<? super OUT> subscriber) {
                try {
                    ctx = Vertx.currentContext();
                    if (!subscriber.isUnsubscribed()) {
                        start(subscriber);
                    }
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
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
    protected void start(Subscriber<? super OUT> subscriber) {
        this.subscriber = subscriber;
        start(getRequest());
    }

    /**
     * @param request
     */
    protected abstract void start(IN request);

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
    protected void complete(Subscriber<? super OUT> subscriber, OUT response) {
        try {
            if (!subscriber.isUnsubscribed()) {
                subscriber.onNext(response);
                subscriber.onCompleted();
            }
        } catch (Exception e) {
            subscriber.onError(e);
        }
    }

    protected <A extends Action<I, O>, I, O> Observable<O> pipe(ActionProvider<A, I, O> provider, Func.Run1<I> inCallback) {
        final I in = provider.getInProvider().get();
        if (inCallback != null) inCallback.run(in);
        return observe(provider, in);
    }

    protected <A extends Action<I, O>, I, O> Observable<O> observeWithSupplier(ActionProvider<A, I, O> provider, Func.Run1<I> inCallback) {
        final I in = provider.getInProvider().get();
        if (inCallback != null) inCallback.run(in);
        return observe(provider, in);
    }

    protected <A extends Action<I, O>, I, O> Observable<O> observe(ActionProvider<A, I, O> provider, I in) {
        return provider.observe(getContext(), in);
    }
}
