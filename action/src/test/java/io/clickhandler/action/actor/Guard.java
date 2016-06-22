package io.clickhandler.action.actor;

import io.clickhandler.action.Actor;
import javaslang.collection.List;
import javaslang.control.Try;
import rx.Observable;
import rx.Subscriber;

/**
 *
 */
public abstract class Guard<T> {
    List<Subscriber<? super T>> subscribers;
    Subscriber<? super T> current;
    private Actor actor;
    private T result;
    private boolean done;

    public Guard(Actor actor) {
        this.actor = actor;
    }

    public boolean isDone() {
        return done;
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
                                try {
                                    result = r;
                                    Try.run(() -> merge(r));
                                    Try.run(() -> onNext(subscriber, r));
                                    current = null;

                                    if (subscribers != null) {
                                        subscribers.forEach(s -> onNext(s, r));
                                        subscribers = null;
                                    }
                                } finally {
                                    done = true;
                                }
                            }
                        },
                        e -> {
                            synchronized (this) {
                                try {
                                    Try.run(() -> merge(e));
                                    Try.run(() -> onError(subscriber, e));
                                    current = null;

                                    if (subscribers != null)
                                        subscribers.forEach(s -> onError(s, e));
                                } finally {
                                    done = true;
                                }
                            }
                        }
                    );
                } else {
                    if (subscribers == null)
                        subscribers = List.empty();

                    subscribers = subscribers.append(subscriber);
                }
            }
        }).observeOn(actor.getScheduler()).subscribeOn(actor.getScheduler());
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
