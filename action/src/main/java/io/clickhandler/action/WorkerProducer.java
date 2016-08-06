package io.clickhandler.action;

import com.google.common.util.concurrent.Service;
import rx.Observable;

/**
 *
 */
public interface WorkerProducer extends Service {
    Observable<Boolean> send(WorkerRequest request);
}
