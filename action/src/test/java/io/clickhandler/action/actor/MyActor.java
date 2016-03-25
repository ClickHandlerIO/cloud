package io.clickhandler.action.actor;

import io.clickhandler.action.AbstractActor;
import javaslang.collection.HashSet;
import javaslang.collection.List;
import javaslang.collection.Set;
import javaslang.control.Try;
import rx.Observable;
import rx.Subscriber;

import javax.inject.Inject;
import java.util.UUID;

/**
 *
 */
public class MyActor extends AbstractActor {
    private Status status = Status.NEW;
    private State state = new State();

    @Inject
    public MyActor() {
    }

    public State state() {
        return state;
    }

    @Override
    protected void started() {

    }

    public Observable<String> load() {
        return state.addWatcher.call();
    }

    enum Status {
        NEW,
    }

    class State {
        Set<String> watchers = HashSet.empty();
        SingletonObservable<String> addWatcher = new SingletonObservable<String>() {
            @Override
            protected Observable<String> makeCall() {
                return Observable.<String>create(subscriber -> {
                    if (subscriber.isUnsubscribed()) {
                        return;
                    }

                    getVertx().setTimer(2000, event -> {
                        try {
                            subscriber.onNext(UUID.randomUUID().toString());
                            subscriber.onCompleted();
                        } catch (Exception e) {
                            subscriber.onError(e);
                        }
                    });

                }).subscribeOn(getScheduler()).observeOn(getScheduler());
            }
        };

        public Set<String> addWatcher(String watcher) {
            return watchers = watchers.add(watcher);
        }

        public Set<String> getWatchers() {
            return watchers;
        }
    }

    public abstract class SingletonObservable<T> {
        List<Subscriber<? super T>> subscribers;
        Subscriber<? super T> current;

        public Observable<T> call() {
            return Observable.<T>create(subscriber -> {
                if (subscriber.isUnsubscribed())
                    return;

                if (current == null) {
                    current = subscriber;
                    makeCall().subscribe(
                        result -> {
                            onNext(subscriber, result);
                            current = null;

                            if (subscribers != null)
                                subscribers.forEach(s -> onNext(s, result));
                        },
                        e -> {
                            onError(subscriber, e);
                            current = null;

                            if (subscribers != null)
                                subscribers.forEach(s -> onError(s, e));
                        }
                    );
                } else {
                    if (subscribers == null)
                        subscribers = List.empty();
                    subscribers = subscribers.append(subscriber);
                }
            }).observeOn(getScheduler()).subscribeOn(getScheduler());
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

        protected abstract Observable<T> makeCall();
    }
}
