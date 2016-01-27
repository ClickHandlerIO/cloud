package io.clickhandler.action;

import javaslang.control.Try;
import rx.Subscriber;

import javax.inject.Inject;

/**
 *
 */
@InternalAction
@ActionConfig(maxExecutionMillis = 500)
public class MyAsyncAction extends AbstractObservableAction<String, String> {
    @Inject
    public MyAsyncAction() {
    }

    @Override
    protected void start(Subscriber<? super String> subscriber) {
//        Try.run(() -> Thread.sleep(5000));
        subscriber.onNext(getRequest() + " - Back at You");
    }
}
