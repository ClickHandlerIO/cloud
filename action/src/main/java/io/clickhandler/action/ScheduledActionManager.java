package io.clickhandler.action;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.AbstractScheduledService;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.shareddata.Lock;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

/**
 *
 */
@Singleton
public class ScheduledActionManager extends AbstractIdleService {
    @Inject
    Vertx vertx;
    @Inject
    ActionManager actionManager;

    @Inject
    ScheduledActionManager() {
    }

    @Override
    protected void startUp() throws Exception {

    }

    @Override
    protected void shutDown() throws Exception {

    }

    private class ClusterSingleton extends AbstractExecutionThreadService {
        private final ScheduledActionProvider provider;
        private Lock lock;

        public ClusterSingleton(ScheduledActionProvider provider) {
            this.provider = provider;
        }

        @Override
        protected void startUp() throws Exception {

        }

        @Override
        protected void run() throws Exception {
            while (isRunning()) {
                try {
                    final Lock lock = vertx.sharedData()
                        .getLockObservable(provider.getActionClass().getCanonicalName())
                        .toBlocking()
                        .single();

                    try {
                        while (true) {
                            provider.observe(new Object()).subscribe(
                                r -> {
                                },
                                e -> {
                                }
                            );

                            Thread.sleep(provider.getScheduledAction().delaySeconds() * 1000);
                        }
                    } finally {
                        lock.release();
                    }
                } catch (Throwable e) {
                    // Ignore.
                }
            }
        }
    }

    private class NodeSingleton extends AbstractScheduledService {
        private final ScheduledActionProvider provider;

        public NodeSingleton(ScheduledActionProvider provider) {
            this.provider = provider;
        }

        @Override
        protected void runOneIteration() throws Exception {
            try {
                provider.observe(new Object()).subscribe(
                    r -> {
                    },
                    e -> {
                    }
                );
            } catch (Throwable e) {
                // Ignore.
            }
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedDelaySchedule(0, provider.getScheduledAction().delaySeconds(), TimeUnit.SECONDS);
        }
    }
}
