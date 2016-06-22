package io.clickhandler.action.actor;

import io.clickhandler.action.AbstractActor;
import javaslang.collection.HashSet;
import javaslang.collection.Set;
import rx.Observable;

import javax.inject.Inject;
import java.util.UUID;

/**
 *
 */
public class MyActor extends AbstractActor<MyActor.State> {

    @Inject
    public MyActor() {
    }

    /**
     * @return
     */
    @Override
    protected State initialState() {
        return new State();
    }

    public Observable<String> load() {
        return state().load.observe();
    }

    /**
     *
     */
    enum Status {
        NEW,
    }

    /**
     *
     */
    public class State {
        Status status;
        Set<String> watchers = HashSet.empty();

        Guard<MyActorInternalAction.Response> loader = new Guard<MyActorInternalAction.Response>(MyActor.this) {
            @Override
            protected Observable<MyActorInternalAction.Response> execute() {
//                Main.actions().actor().
                return null;
            }

            @Override
            protected void merge(MyActorInternalAction.Response result) {

            }

            @Override
            protected void merge(Throwable e) {

            }
        };

        Guard<String> load = new Guard<String>(MyActor.this) {
            @Override
            protected Observable<String> execute() {
                return Observable.create(subscriber -> {
                    if (subscriber.isUnsubscribed()) {
                        return;
                    }

                    try {
                        subscriber.onNext(UUID.randomUUID().toString());
                        subscriber.onCompleted();
                    } catch (Exception e) {
                        subscriber.onError(e);
                    }

//                    getVertx().setTimer(0, event -> {
//                        try {
//                            subscriber.onNext(UUID.randomUUID().toString());
//                            subscriber.onCompleted();
//                        } catch (Exception e) {
//                            subscriber.onError(e);
//                        }
//                    });

                });//.subscribeOn(getScheduler()).observeOn(getScheduler());
            }

            @Override
            protected void merge(String result) {
                System.out.println();
                System.out.println();
                System.err.println("merge() -> " + Thread.currentThread().getName());
                watchers = watchers.add(result);
            }

            @Override
            protected void merge(Throwable e) {
            }
        };

        public Set<String> addWatcher(String watcher) {
            return watchers = watchers.add(watcher);
        }

        public Set<String> getWatchers() {
            return watchers;
        }
    }

}
