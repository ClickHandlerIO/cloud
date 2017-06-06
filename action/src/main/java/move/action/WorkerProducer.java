package move.action;

import rx.Single;

/**
 *
 */
public interface WorkerProducer {
    Single<Boolean> send(WorkerRequest request);
}
