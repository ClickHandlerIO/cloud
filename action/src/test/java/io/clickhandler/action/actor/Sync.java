package io.clickhandler.action.actor;

import javaslang.collection.List;
import javaslang.control.Try;
import rx.Observable;
import rx.Subscriber;

/**
 *
 */
public abstract class Sync<T> {
    List<Subscriber<? super T>> subscribers;
    Subscriber<? super T> current;
    private MyActor myActor;
    private T result;

    public Sync(MyActor myActor) {
        this.myActor = myActor;
    }

    public Observable<T> observe() {
        if (result != null)
            return Observable.just(result);

        return Observable.<T>create(subscriber -> {
            if (subscriber.isUnsubscribed())
                return;

            synchronized (this) {
                if (result != null) {
                    Try.run(() -> {
                        subscriber.onNext(result);
                        subscriber.onCompleted();
                    }).recoverWith(e ->
                        Try.run(() -> subscriber.onError(e))
                    );
                    return;
                }

                if (current == null) {
                    current = subscriber;
                    execute().subscribe(
                        r -> {
                            synchronized (this) {
                                result = r;
                                Try.run(() -> merge(r));
                                onNext(subscriber, r);
                                current = null;

                                if (subscribers != null) {
                                    subscribers.forEach(s -> onNext(s, r));
                                    subscribers = null;
                                }
                            }
                        },
                        e -> {
                            synchronized (this) {
                                merge(e);
                                onError(subscriber, e);
                                current = null;

                                if (subscribers != null)
                                    subscribers.forEach(s -> onError(s, e));
                            }
                        }
                    );
                } else {
                    if (subscribers == null)
                        subscribers = List.empty();

                    subscribers = subscribers.append(subscriber);
                }
            }
        }).observeOn(myActor.getScheduler()).subscribeOn(myActor.getScheduler());
    }

    protected void onNext(Subscriber<? super T> subscriber, T next) {
        if (subscriber.isUnsubscribed())
            return;

        try {
            subscriber.onNext(next);
            subscriber.onCompleted();
        } catch (Throwable e) {
            Try.run(() -> subscriber.onError(e));
        }
    }

    protected void onError(Subscriber<? super T> subscriber, Throwable e) {
        if (subscriber.isUnsubscribed())
            return;

        Try.run(() -> subscriber.onError(e));
    }

    protected abstract Observable<T> execute();

    protected abstract void merge(T result);

    protected abstract void merge(Throwable e);
}
