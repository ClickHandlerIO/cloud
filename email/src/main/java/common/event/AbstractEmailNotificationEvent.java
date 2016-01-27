package common.event;

import event.AbstractVertxEvent;

/**
 * Created by admin on 1/27/16.
 */
public abstract class AbstractEmailNotificationEvent extends AbstractVertxEvent {
    public AbstractEmailNotificationEvent(String eventAddress) {
        super(eventAddress);
    }
}
