package io.clickhandler.email.common.event;

import event.VertxEvent;

/**
 *  Abstract for all events posted to the vertx EventBus.
 *
 *  @see io.vertx.core.eventbus.EventBus
 *  @author Brad Behnke
 */
public abstract class NotificationEvent<T> extends VertxEvent<T>{
    public NotificationEvent(T value, Class<T> valueType) {
        super(value, valueType);
    }
}
