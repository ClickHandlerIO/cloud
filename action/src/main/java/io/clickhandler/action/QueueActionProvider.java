package io.clickhandler.action;

import rx.Observable;

/**
 *
 */
public class QueueActionProvider<A extends Action<IN, OUT>, IN, OUT> extends ActionProvider<A, IN, OUT> {
    private QueueAction queueAction;

    public QueueAction getQueueAction() {
        return queueAction;
    }

    @Override
    protected void init() {
        super.init();
        queueAction = getActionClass().getAnnotation(QueueAction.class);
    }

    public Observable<Boolean> add(final IN request) {
        return Observable.empty();
    }
}
