package sns.event.common;

import event.AbstractVertxEvent;

/**
 * Created by admin on 1/26/16.
 */
public abstract class SNSEvent extends AbstractVertxEvent {
    public SNSEvent(String eventAddress) {
        super(eventAddress);
    }
}
