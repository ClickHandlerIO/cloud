package move.action;

import com.google.common.base.Preconditions;
import javaslang.control.Try;
import rx.Observable;

import javax.inject.Inject;
import java.util.function.Consumer;

/**
 *
 */
public class WorkerActionProvider<A extends Action<IN, Boolean>, IN> extends ActionProvider<A, IN, Boolean> {
    protected WorkerAction workerAction;
    protected WorkerProducer producer;
    protected String name;
    protected boolean fifo;

    @Inject
    public WorkerActionProvider() {
    }

    /**
     * @return
     */
    public String getName() {
        return name;
    }

    public boolean isFifo() {
        return fifo;
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
        fifo = workerAction.fifo();
        super.init();
    }

    /**
     * @param request
     * @param callback
     */
    public void send(IN request, Consumer<Boolean> callback) {
        send(request, 0, callback);
    }

    /**
     * @param request
     * @param delaySeconds
     * @param callback
     */
    public void send(IN request, int delaySeconds, Consumer<Boolean> callback) {
        send(request, delaySeconds).subscribe(
            r -> Try.run(() -> callback.accept(r)),
            e -> Try.run(() -> callback.accept(false))
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
        return producer.send(new WorkerRequest()
            .actionProvider(this)
            .request(request)
            .delaySeconds(delaySeconds));
    }
}
