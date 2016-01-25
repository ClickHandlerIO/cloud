package io.clickhandler.queue;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 *
 */
public abstract class AbstractBackingQueueService<T> extends AbstractExecutionThreadService implements QueueService<T> {
    protected final QueueServiceConfig<T> config;
    protected final String name;
    protected final Class<T> type;
    protected final QueueHandler<T> handler;
    protected final int parallelism;
    protected final int maxMessages;
    protected final ExecutorService executorService;
    protected final Logger log;

    public AbstractBackingQueueService(QueueServiceConfig<T> config) {
        this.config = com.google.common.base.Preconditions.checkNotNull(config, "config was null");
        this.name = com.google.common.base.Preconditions.checkNotNull(config.getName(), "name was null");
        this.type = com.google.common.base.Preconditions.checkNotNull(config.getType(), "type was null");
        this.handler = com.google.common.base.Preconditions.checkNotNull(config.getHandler(), "handler was null");
        this.executorService = com.google.common.base.Preconditions.checkNotNull(config.getExecutorService(), "executorService was null");
        this.parallelism = config.getParallelism();
        this.maxMessages = config.getBatchSize() < 1 ? 1 : config.getBatchSize() > 10 ? 10 : config.getBatchSize();
        this.log = LoggerFactory.getLogger(getClass().getCanonicalName() + "[" + name + "]");
    }

    @Override
    protected void run() throws Exception {
        if (parallelism == 1) {
            while (isRunning()) {
                try {
                    doRun();
                } catch (Exception e) {
                    log.error("doRun() threw an unexpected exception.", e);
                }
            }
        } else {
            final List<Callable<Void>> callables = Lists.newArrayListWithCapacity(parallelism);
            while (isRunning()) {
                try {
                    for (int i = 0; i < parallelism; i++) {
                        callables.add(() -> {
                            try {
                                doRun();
                            } catch (Exception e) {
                                log.error("doRun() threw an unexpected exception.", e);
                            }
                            return null;
                        });
                    }
                    final List<Future<Void>> futures = executorService.invokeAll(callables);
                    for (Future<Void> future : futures) {
                        future.get();
                    }
                } finally {
                    callables.clear();
                }
            }
        }
    }

    protected abstract void doRun() throws Exception;
}
