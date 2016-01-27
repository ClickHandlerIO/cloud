package common.event;

import event.AbstractVertxEvent;

/**
 * Created by admin on 1/27/16.
 */
public class AbstractNotificationEvent extends AbstractVertxEvent {
    public AbstractNotificationEvent(String eventAddress) {
        super(eventAddress);
    }
}
