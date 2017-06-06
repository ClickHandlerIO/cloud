package move.action;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.AbstractIdleService;
import io.vertx.rxjava.core.Vertx;
import javaslang.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Single;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 *
 */
@Singleton
public class LocalWorkerService extends AbstractIdleService implements WorkerService, WorkerProducer, WorkerConsumer {
    static final Logger LOG = LoggerFactory.getLogger(LocalWorkerService.class);

    private final LinkedBlockingDeque<WorkerRequest> queue = new LinkedBlockingDeque<>();
    private final Consumer consumer = new Consumer();

    @Inject
    Vertx vertx;

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
    public Single<Boolean> send(WorkerRequest request) {
        return Single.create(subscriber -> {
            if (request.delaySeconds > 0) {
                vertx.setTimer(
                    TimeUnit.SECONDS.toMillis(request.delaySeconds),
                    event -> queue.add(request)
                );
            }
            else {
                queue.add(request);
            }

            subscriber.onSuccess(true);
        });
    }

    private final class Consumer extends AbstractExecutionThreadService {
        private Thread thread;

        @Override
        protected void triggerShutdown() {
            Try.run(() -> thread.interrupt());
        }

        @Override
        protected void run() throws Exception {
            thread = Thread.currentThread();

            while (isRunning()) {
                try {
                    doRun();
                }
                catch (InterruptedException e) {
                    return;
                }
                catch (Throwable e) {
                    // Ignore.
                    LOG.error("Unexpected exception", e);
                }
            }
        }

        protected void doRun() throws InterruptedException {
            final WorkerRequest request = queue.take();
            if (request == null) {
                return;
            }

            request.actionProvider.execute(request.request);
        }
    }
}
