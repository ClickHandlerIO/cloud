package common.event;


/**
 *  Abstract for email notifications posted to the vertx EventBus.
 *
 *  @see io.vertx.core.eventbus.EventBus
 *  @author Brad Behnke
 */
public abstract class AbstractEmailNotificationEvent extends AbstractNotificationEvent {
    public AbstractEmailNotificationEvent(String eventAddress) {
        super(eventAddress);
    }
}
