package common.event;

import event.VertxEvent;

/**
 *  Abstract for all events posted to the vertx EventBus.
 *
 *  @see io.vertx.core.eventbus.EventBus
 *  @author Brad Behnke
 */
public class NotificationEvent extends VertxEvent {
    public NotificationEvent(String eventAddress) {
        super(eventAddress);
    }
}
