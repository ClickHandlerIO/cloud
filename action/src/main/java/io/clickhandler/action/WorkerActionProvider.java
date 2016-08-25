package io.clickhandler.action;

import com.google.common.base.Preconditions;
import io.clickhandler.common.Func;
import javaslang.control.Try;
import rx.Observable;

import javax.inject.Inject;

/**
 *
 */
public class WorkerActionProvider<A extends Action<IN, Boolean>, IN> extends ActionProvider<A, IN, Boolean> {
    private WorkerAction workerAction;
    private WorkerProducer producer;
    private String name;

    @Inject
    public WorkerActionProvider() {
    }

    /**
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * @return
     */
    public String getQueueName() {
        return workerAction != null ? workerAction.queueName() : "";
    }

    /**
     * @param producer
     */
    void setProducer(WorkerProducer producer) {
        this.producer = producer;
    }

    public WorkerAction getWorkerAction() {
        if (workerAction == null) {
            workerAction = getActionClass().getAnnotation(WorkerAction.class);
        }
        return workerAction;
    }

    @Override
    protected void init() {
        workerAction = getActionClass().getAnnotation(WorkerAction.class);
        name = getActionClass().getCanonicalName();
        super.init();
    }

    /**
     * @param request
     * @param callback
     */
    public void send(IN request, Func.Run1<Boolean> callback) {
        send(request, 0, callback);
    }

    /**
     * @param request
     * @param delaySeconds
     * @param callback
     */
    public void send(IN request, int delaySeconds, Func.Run1<Boolean> callback) {
        send(request, delaySeconds).subscribe(
            r -> Try.run(() -> callback.run(r)),
            e -> Try.run(() -> callback.run(false))
        );
    }

    /**
     * @param request
     * @return
     */
    public Observable<Boolean> send(IN request) {
        return send(request, 0);
    }

    /**
     * @param request
     * @param delaySeconds
     * @return
     */
    public Observable<Boolean> send(IN request, int delaySeconds) {
        Preconditions.checkNotNull(
            producer,
            "WorkerProducer is null. Ensure ActionManager has been started and all actions have been registered."
        );
        return producer.send(new WorkerRequest().actionProvider(this).request(request).delaySeconds(delaySeconds));
    }

    /**
     * @param request
     * @param delaySeconds
     * @return
     */
    public Observable<Boolean> send(IN request, String throttleKey, int delaySeconds) {
        Preconditions.checkNotNull(
            producer,
            "WorkerProducer is null. Ensure ActionManager has been started and all actions have been registered."
        );
        return producer.send(new WorkerRequest().actionProvider(this).request(request).delaySeconds(delaySeconds));
    }
}
