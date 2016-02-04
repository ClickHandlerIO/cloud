package io.clickhandler.email.common.event;


/**
 *  Abstract for email notifications posted to the vertx EventBus.
 *
 *  @see io.vertx.core.eventbus.EventBus
 *  @author Brad Behnke
 */
public abstract class EmailNotificationEvent<T> extends NotificationEvent<T> {
    public EmailNotificationEvent(T value, Class<T> valueType) {
        super(value, valueType);
    }
}
