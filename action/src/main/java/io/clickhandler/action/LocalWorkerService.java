package io.clickhandler.action;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.AbstractIdleService;
import io.vertx.rxjava.core.Vertx;
import rx.Observable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 *
 */
@Singleton
public class LocalWorkerService extends AbstractIdleService implements WorkerService, WorkerProducer, WorkerConsumer {
    private final LinkedBlockingDeque<WorkerRequest> queue = new LinkedBlockingDeque<>();
    @Inject
    Vertx vertx;

    private final Consumer consumer = new Consumer();

    @Inject
    LocalWorkerService() {
    }

    @Override
    protected void startUp() throws Exception {
        ActionManager.getWorkerActionMap().values().forEach(provider -> provider.setProducer(this));
        consumer.startAsync().awaitRunning();
    }

    @Override
    protected void shutDown() throws Exception {
        consumer.stopAsync().awaitTerminated();
    }

    @Override
    public Observable<Boolean> send(WorkerRequest request) {
        return Observable.create(subscriber -> {
            if (request.delaySeconds > 0) {
                vertx.setTimer(TimeUnit.SECONDS.toMillis(request.delaySeconds), event -> {
                    queue.add(request);
                });
            } else {
                queue.add(request);
            }

            subscriber.onNext(true);
            subscriber.onCompleted();
        });
    }

    private final class Consumer extends AbstractExecutionThreadService {
        @Override
        protected void run() throws Exception {
            while (isRunning()) {
                try {
                    final WorkerRequest request = queue.take();
                    if (request == null)
                        continue;

                    request.actionProvider.observe(request.request).subscribe();
                } catch (Throwable e) {
                    // Ignore.
                }
            }
        }
    }
}
