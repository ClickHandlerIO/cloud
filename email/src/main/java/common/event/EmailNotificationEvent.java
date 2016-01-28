package common.event;


/**
 *  Abstract for email notifications posted to the vertx EventBus.
 *
 *  @see io.vertx.core.eventbus.EventBus
 *  @author Brad Behnke
 */
public abstract class EmailNotificationEvent extends NotificationEvent {
    public EmailNotificationEvent(String eventAddress) {
        super(eventAddress);
    }
}
