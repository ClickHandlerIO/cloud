package move.action;

import rx.Single;

/**
 *
 */
public interface WorkerProducer {
    Single<WorkerReceipt> send(WorkerRequest request);
}