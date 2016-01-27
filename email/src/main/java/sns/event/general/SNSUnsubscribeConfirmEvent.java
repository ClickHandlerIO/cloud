package sns.event.general;

import common.event.AbstractNotificationEvent;
import sns.json.general.GeneralMessage;

/**
 * Created by admin on 1/25/16.
 */
public class SNSUnsubscribeConfirmEvent extends AbstractNotificationEvent {
    public static final String ADDRESS = "sns-unsubscribe";
    private GeneralMessage message;

    public SNSUnsubscribeConfirmEvent() {
        super(ADDRESS);
    }

    public SNSUnsubscribeConfirmEvent(GeneralMessage message) {
        super(ADDRESS);
        this.message = message;
    }

    public GeneralMessage getMessage() {
        return message;
    }

    public void setMessage(GeneralMessage message) {
        this.message = message;
    }
}
