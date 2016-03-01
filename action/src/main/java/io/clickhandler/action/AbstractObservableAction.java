package io.clickhandler.action;

import com.netflix.hystrix.HystrixObservableCommand;
import rx.Observable;
import rx.Subscriber;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Clay Molocznik
 */
public abstract class AbstractObservableAction<IN, OUT>
    extends AbstractAction<IN, OUT>
    implements ObservableAction<IN, OUT> {

    private final AtomicReference<HystrixObservableCommand<OUT>> command = new AtomicReference<>();
    private HystrixObservableCommand.Setter setter;

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
        final HystrixObservableCommand<OUT> existing = command.get();
        if (existing != null) {
            return existing;
        }

        final HystrixObservableCommand<OUT> newCommand = build();
        if (!command.compareAndSet(null, newCommand)) {
            return command.get();
        } else {
            return newCommand;
        }
    }

    /**
     * @param subscriber
     */
    protected abstract void start(Subscriber<? super OUT> subscriber);

    /**
     * @param subscriber
     * @param e
     */
    protected void error(Subscriber<? super OUT> subscriber, Throwable e) {
        try {
            if (!subscriber.isUnsubscribed()) {
                subscriber.onError(e);
            }
        } catch (Exception e1) {
            subscriber.onError(e);
        }
    }

    /**
     * @param subscriber
     * @param response
     */
    protected void next(Subscriber<? super OUT> subscriber, OUT response) {
        try {
            if (!subscriber.isUnsubscribed()) {
                subscriber.onNext(response);
            }
        } catch (Exception e) {
            subscriber.onError(e);
        }
    }

    /**
     * @param subscriber
     */
    protected void complete(Subscriber<? super OUT> subscriber) {
        try {
            if (!subscriber.isUnsubscribed()) {
                subscriber.onCompleted();
            }
        } catch (Exception e) {
            subscriber.onError(e);
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

    protected <A extends Action<I, O>, I, O> Observable<O> observe(ActionProvider<A, I, O> provider, I in) {
        return provider.observe(getContext(), in);
    }
}
