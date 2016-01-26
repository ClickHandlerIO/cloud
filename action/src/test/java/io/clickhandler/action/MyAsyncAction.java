package io.clickhandler.action;

import rx.Subscriber;

import javax.inject.Inject;

/**
 *
 */
@InternalAction
public class MyAsyncAction extends AbstractObservableAction<String, String> {
    @Inject
    public MyAsyncAction() {
    }

    @Override
    protected void start(Subscriber<? super String> subscriber) {
        subscriber.onNext(getRequest() + " - Back at You");
    }
}
