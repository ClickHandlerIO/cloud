package io.clickhandler.queue;

/**
 *
 */
public class LocalQueueServiceFactory implements QueueFactory {
    public static void main(String[] args) {


    }

    @Override
    public <T> QueueService<T> build(QueueServiceConfig<T> config) {
        return new LocalQueueService<>(config);
    }


}
