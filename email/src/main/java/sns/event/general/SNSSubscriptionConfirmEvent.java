package sns.event.general;

import common.event.NotificationEvent;
import sns.data.json.general.GeneralMessage;

/**
 * Vertx SNS Subcription Confirmation Event
 *
 * @author Brad Behnke
 */
public class SNSSubscriptionConfirmEvent extends NotificationEvent {
    public static final String ADDRESS = "sns-subscribe";
    private GeneralMessage message;

    public SNSSubscriptionConfirmEvent() {
        super(ADDRESS);
    }

    public SNSSubscriptionConfirmEvent(GeneralMessage message) {
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
