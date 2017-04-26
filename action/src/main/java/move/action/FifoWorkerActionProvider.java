package move.action;

import com.google.common.base.Preconditions;
import javaslang.control.Try;
import rx.Observable;

import javax.inject.Inject;
import java.util.function.Consumer;

/**
 *
 */
public class FifoWorkerActionProvider<A extends Action<IN, Boolean>, IN> extends WorkerActionProvider<A, IN> {
    @Inject
    public FifoWorkerActionProvider() {
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
     * @param groupId
     * @param callback
     */
    public void send(IN request, String groupId, Consumer<Boolean> callback) {
        send(request, groupId).subscribe(
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

    /**
     * @param request
     * @param groupId
     * @return
     */
    public Observable<Boolean> send(IN request, String groupId) {
        Preconditions.checkNotNull(
            producer,
            "WorkerProducer is null. Ensure ActionManager has been started and all actions have been registered."
        );
        return producer.send(new WorkerRequest()
            .actionProvider(this)
            .request(request)
            .groupId(groupId));
    }

    /**
     * @param request
     * @param delaySeconds
     * @return
     */
    public Observable<Boolean> send(IN request, String groupId, int delaySeconds) {
        Preconditions.checkNotNull(
            producer,
            "WorkerProducer is null. Ensure ActionManager has been started and all actions have been registered."
        );
        return producer.send(new WorkerRequest()
            .actionProvider(this)
            .request(request)
            .groupId(groupId)
            .delaySeconds(delaySeconds));
    }
}
