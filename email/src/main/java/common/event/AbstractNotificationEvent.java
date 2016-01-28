package common.event;

import event.AbstractVertxEvent;

/**
 *  Abstract for all notifications posted to the vertx EventBus.
 *
 *  @see io.vertx.core.eventbus.EventBus
 *  @author Brad Behnke
 */
public class AbstractNotificationEvent extends AbstractVertxEvent {
    public AbstractNotificationEvent(String eventAddress) {
        super(eventAddress);
    }
}
