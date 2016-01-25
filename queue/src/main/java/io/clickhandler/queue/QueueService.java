package io.clickhandler.queue;

import com.google.common.util.concurrent.Service;

import java.util.Collection;

/**
 * @author Clay Molocznik
 */
public interface QueueService<T> extends Service {
    void add(T message);

    void addAll(Collection<T> collection);
}
