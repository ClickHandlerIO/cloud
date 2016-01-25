package io.clickhandler.queue;

import javaslang.control.Try;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class LocalQueueService<T> extends AbstractBackingQueueService<T> {
    private BlockingQueue<T> queue;

    public LocalQueueService(QueueServiceConfig<T> config) {
        super(config);
    }

    @Override
    protected void startUp() throws Exception {
        queue = new LinkedBlockingDeque<>();
        super.startUp();
    }

    @Override
    protected void shutDown() throws Exception {
        queue.clear();
    }

    protected void doRun() {
        final ArrayList<T> messages = new ArrayList<>(maxMessages);

        while (isRunning() && messages.size() < maxMessages) {
            try {
                // Try emptying some messages from the Queue.
                queue.drainTo(messages, maxMessages - messages.size());

                // Should we process some
                if (!messages.isEmpty()) {
                    break;
                }

                // Wait for a single message indefinitely.
                final T message = queue.poll(1, TimeUnit.SECONDS);
                if (message != null) {
                    messages.add(message);
                }
            } catch (InterruptedException e) {
                return;
            } catch (Exception e) {
                log.error("Unexpected exception whiling polling the Queue [" + name + "]", e);
                // Provide a little processing relief if this keeps happening.
                Try.run(() -> Thread.sleep(1000));
            }
        }

        if (!isRunning() || messages.isEmpty()) {
            return;
        }

        try {
            handler.receive(messages);
        } catch (Exception e) {
            final Throwable cause = e.getCause();
            if ((cause != null && cause instanceof InterruptedException)) {
                return;
            }
            log.error("receive(List<T> messages) throw an unexpected exception.");
            Try.run(() -> Thread.sleep(1000));
        }
    }

    @Override
    public void add(T message) {
        queue.add(message);
    }

    @Override
    public void addAll(Collection<T> collection) {
        queue.addAll(collection);
    }
}
