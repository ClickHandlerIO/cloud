package io.clickhandler.action;

import rx.Subscriber;

import javax.inject.Inject;

/**
 *
 */
//@InternalAction
//@RemoteAction(path = "/some/path")
public class AllocateInventory extends AbstractObservableAction<String, String> {
    @Inject
    public AllocateInventory() {
    }

    @Override
    protected void start(Subscriber<? super String> subscriber) {
        subscriber.onNext("TEST");
    }
}
