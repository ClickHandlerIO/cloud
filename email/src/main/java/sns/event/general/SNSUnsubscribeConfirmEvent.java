package sns.event.general;

import common.event.NotificationEvent;
import sns.data.json.general.GeneralMessage;

/**
 * Vertx SNS Unsubscribe Confirmation Event
 *
 * @author Brad Behnke
 */
public class SNSUnsubscribeConfirmEvent extends NotificationEvent {
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
