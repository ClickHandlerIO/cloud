package io.clickhandler.action;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import io.clickhandler.cloud.cluster.HazelcastProvider;
import io.vertx.rxjava.core.Vertx;
import javaslang.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOG = LoggerFactory.getLogger(ScheduledActionManager.class);
    private final List<ClusterSingleton> clusterSingletons = new ArrayList<>();
    private final List<NodeSingleton> nodeSingletons = new ArrayList<>();
    @Inject
    Vertx vertx;
    @Inject
    HazelcastProvider hazelcastProvider;
    HazelcastInstance hazelcastInstance;

    @Inject
    ScheduledActionManager() {
    }

    @Override
    protected void startUp() throws Exception {
        hazelcastInstance = hazelcastProvider != null ? hazelcastProvider.get() : null;
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

    /**
     *
     */
    private class ClusterSingleton extends AbstractExecutionThreadService {
        private final ScheduledActionProvider provider;
        private final int intervalSeconds;
        private Thread thread;

        public ClusterSingleton(ScheduledActionProvider provider) {
            this.provider = provider;
            this.intervalSeconds = provider.getScheduledAction().intervalSeconds();

            Preconditions.checkNotNull(intervalSeconds, "ScheduledAction: " +
                provider.getActionClass().getCanonicalName() +
                " has an invalid value for intervalSeconds() = " +
                intervalSeconds);
        }

        @Override
        protected String serviceName() {
            return provider.getActionClass().getCanonicalName();
        }

        @Override
        protected void startUp() throws Exception {
        }

        @Override
        protected void triggerShutdown() {
            Try.run(() -> thread.interrupt());
        }

        @Override
        protected void run() throws Exception {
            thread = Thread.currentThread();
            while (isRunning() && !Thread.interrupted()) {
                try {
                    if (hazelcastInstance == null) {
                        while (isRunning()) {
                            doRun();
                        }
                    } else {
                        final ILock lock = hazelcastInstance.getLock(provider.getActionClass().getCanonicalName());
                        lock.lockInterruptibly();
                        try {
                            while (isRunning() && !Thread.interrupted()) {
                                doRun();
                            }
                        } catch (InterruptedException e) {
                            return;
                        } finally {
                            lock.unlock();
                        }
                    }
                } catch (InterruptedException e) {
                    LOG.warn(provider.getActionClass().getCanonicalName(), e);
                } catch (Throwable e) {
                    // Ignore.
                    LOG.warn("Failed to get Cluster lock for " + provider.getActionClass().getCanonicalName(), e);
                }
            }
        }

        private void doRun() throws InterruptedException {
            final long start = System.currentTimeMillis();
            provider.observe(null).toBlocking().first();

            final long elapsed = System.currentTimeMillis() - start;
            final long sleepFor = TimeUnit.SECONDS.toMillis(intervalSeconds) - elapsed;

            try {
                if (sleepFor > 0)
                    Thread.sleep(sleepFor);
            } catch (InterruptedException e) {
                // Do nothing.
                throw e;
            }
        }
    }

    /**
     *
     */
    private class NodeSingleton extends AbstractScheduledService {
        private final ScheduledActionProvider provider;

        public NodeSingleton(ScheduledActionProvider provider) {
            this.provider = provider;
        }

        @Override
        protected String serviceName() {
            return provider.getActionClass().getCanonicalName();
        }

        @Override
        protected void runOneIteration() throws Exception {
            try {
                run();
            } catch (InterruptedException e) {
                LOG.warn(provider.getActionClass().getCanonicalName(), e);
                Throwables.propagate(e);
            } catch (Throwable e) {
                LOG.warn(provider.getActionClass().getCanonicalName(), e);
            }
        }

        protected void run() throws InterruptedException {
            provider.observe(null).toBlocking().first();
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedRateSchedule(
                0,
                provider.getScheduledAction().intervalSeconds(),
                TimeUnit.SECONDS
            );
        }
    }
}
