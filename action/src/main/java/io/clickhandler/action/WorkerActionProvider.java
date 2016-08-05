package io.clickhandler.action;

import rx.Observable;

import javax.inject.Inject;

/**
 *
 */
public class WorkerActionProvider<A extends Action<IN, Boolean>, IN> extends ActionProvider<A, IN, Boolean> {
    WorkerSender sender;
    private WorkerAction workerAction;
    private String type;

    @Inject
    public WorkerActionProvider() {
    }

    public String getQueueName() {
        return workerAction != null ? workerAction.queueName() : "";
    }

    public String getType() {
        return type;
    }

    void setSender(WorkerSender sender) {
        this.sender = sender;
    }

    @Override
    protected void init() {
        workerAction = getActionClass().getAnnotation(WorkerAction.class);
        type = getActionClass().getCanonicalName();
        super.init();
    }

    public Observable<Boolean> send(IN request) {
        return send(request, 0);
    }

    public Observable<Boolean> send(IN request, int delaySeconds) {
        final WorkerRequest workerRequest = new WorkerRequest().delaySeconds(delaySeconds).actionProvider(this);
        if (request != null)
            workerRequest.payload(request);
        return sender.send(workerRequest);
    }
}
