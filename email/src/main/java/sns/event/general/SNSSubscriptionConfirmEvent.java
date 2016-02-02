package sns.event.general;

import common.event.NotificationEvent;
import sns.data.json.general.GeneralMessage;

/**
 * Vertx SNS Subcription Confirmation Event
 *
 * @author Brad Behnke
 */
public class SNSSubscriptionConfirmEvent extends NotificationEvent<GeneralMessage> {
    public static final String ADDRESS = "sns-subscribe";

    public SNSSubscriptionConfirmEvent() {
        super(null, GeneralMessage.class);
    }

    public SNSSubscriptionConfirmEvent(GeneralMessage message) {
        super(message, GeneralMessage.class);
    }
}
