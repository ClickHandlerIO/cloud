package io.clickhandler.action;

import rx.Observable;

import javax.inject.Inject;

/**
 *
 */
public class WorkerActionProvider<A extends Action<IN, Boolean>, IN> extends ActionProvider<A, IN, Boolean> {
    private static final Object DEFAULT_CONTEXT = new Object();

    private WorkerAction workerAction;

    @Inject
    public WorkerActionProvider() {
    }

    @Override
    protected void init() {
        workerAction = getActionClass().getAnnotation(WorkerAction.class);
        super.init();
    }

    public Observable<Boolean> send(IN request) {
        return send(request, 0);
    }

    public Observable<Boolean> send(IN request, int delaySeconds) {
        return Observable.create(subscriber -> {

        });
    }
}
