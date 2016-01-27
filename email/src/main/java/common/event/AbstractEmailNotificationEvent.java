package common.event;


/**
 * Created by admin on 1/27/16.
 */
public abstract class AbstractEmailNotificationEvent extends AbstractNotificationEvent {
    public AbstractEmailNotificationEvent(String eventAddress) {
        super(eventAddress);
    }
}
