package io.clickhandler.queue;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractIdleService;
import javaslang.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
 *
 */
public abstract class AbstractQueueService<T> extends AbstractIdleService implements QueueService<T> {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final QueueFactory factory;
    private final QueueServiceConfig<T> config;
    private QueueService<T> service;

    public AbstractQueueService(QueueFactory factory, QueueServiceConfig<T> config) {
        this.factory = Preconditions.checkNotNull(factory, "factory was null");
        this.config = Preconditions.checkNotNull(config, "config was null");
        config.setHandler(this::receive);
    }

    @Override
    protected void startUp() throws Exception {
        service = factory.build(config);
        service.startAsync().awaitRunning();
    }

    @Override
    protected void shutDown() throws Exception {
        service.stopAsync().awaitTerminated();
        service = null;
    }

    @Override
    public void add(T message) {
        final QueueService<T> service = this.service;
        if (service != null) {
            service.add(message);
        }
    }

    @Override
    public void addAll(Collection<T> collection) {
        final QueueService<T> service = this.service;
        if (service != null) {
            service.addAll(collection);
        }
    }

    protected void receive(List<T> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        for (final T message : messages) {
            Try.run(() -> receive(message))
                    .onFailure((e) -> log.error("receive(message) threw an exception", e));
        }
    }

    protected abstract void receive(T message);
}
