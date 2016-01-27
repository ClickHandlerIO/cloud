package sns.event.general;

import common.event.AbstractNotificationEvent;
import sns.json.general.GeneralMessage;

/**
 * Created by admin on 1/25/16.
 */
public class SNSNotificationEvent extends AbstractNotificationEvent {
    public static final String ADDRESS = "sns-notification";
    private GeneralMessage message;

    public SNSNotificationEvent() {
        super(ADDRESS);
    }

    public SNSNotificationEvent(GeneralMessage message) {
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
