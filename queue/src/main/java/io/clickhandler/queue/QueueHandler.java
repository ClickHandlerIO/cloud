package io.clickhandler.queue;

import java.util.List;

/**
 *
 * @author Clay Molocznik
 */
public interface QueueHandler<T> {
    void receive(List<T> messages);
}
