package io.clickhandler.action;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.AbstractScheduledService;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.shareddata.Lock;
import javaslang.control.Try;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *
 */
@Singleton
public class ScheduledActionManager extends AbstractIdleService {
    @Inject
    Vertx vertx;

    private List<ClusterSingleton> clusterSingletons = new ArrayList<>();
    private List<NodeSingleton> nodeSingletons = new ArrayList<>();

    @Inject
    ScheduledActionManager() {
    }

    @Override
    protected void startUp() throws Exception {
        ActionManager.getScheduledActionMap().values().forEach(scheduledActionProvider -> {
            scheduledActionProvider.init();
            switch (scheduledActionProvider.getScheduledAction().type()) {
                case CLUSTER_SINGLETON:
                    clusterSingletons.add(new ClusterSingleton(scheduledActionProvider));
                    break;
                case NODE_SINGLETON:
                    nodeSingletons.add(new NodeSingleton(scheduledActionProvider));
                    break;
            }
        });

        nodeSingletons.forEach(value -> value.startAsync().awaitRunning());
        clusterSingletons.forEach(value -> value.startAsync().awaitRunning());
    }

    @Override
    protected void shutDown() throws Exception {
        clusterSingletons.forEach(value -> value.stopAsync().awaitTerminated());
        nodeSingletons.forEach(value -> value.stopAsync().awaitTerminated());
    }

    private class ClusterSingleton extends AbstractExecutionThreadService {
        private final ScheduledActionProvider provider;

        public ClusterSingleton(ScheduledActionProvider provider) {
            this.provider = provider;
        }

        @Override
        protected void startUp() throws Exception {
            System.err.println("starting...");
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
                            final long start = System.currentTimeMillis();

                            try {
                                provider.observe(null).toBlocking().single();
                            } catch (Throwable e) {
                                // Ignore.
                                e.printStackTrace();
                            }

                            final long elapsed = System.currentTimeMillis() - start;
                            Try.run(() -> Thread.sleep(TimeUnit.SECONDS
                                .toMillis(provider.getScheduledAction().intervalSeconds()) - elapsed));
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
                provider.observe(new Object()).toBlocking().single();
            } catch (Throwable e) {
                // Ignore.
            }
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedRateSchedule(0, provider.getScheduledAction().intervalSeconds(), TimeUnit.SECONDS);
        }
    }
}
