package io.clickhandler.queue;

/**
 *
 */
public class AmazonSQSServiceFactory implements QueueFactory {
    @Override
    public <T> QueueService<T> build(QueueServiceConfig<T> config) {
        return new AmazonSQSService<>(config);
    }
}
