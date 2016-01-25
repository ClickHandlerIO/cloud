package io.clickhandler.queue;

/**
 *
 */
public interface QueueFactory {
    /**
     * @param request
     * @param <T>
     * @return
     */
    <T> QueueService<T> build(QueueServiceConfig<T> request);
}
