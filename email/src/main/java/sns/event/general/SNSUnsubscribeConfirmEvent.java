package sns.event.general;

import common.event.NotificationEvent;
import sns.data.json.general.GeneralMessage;

/**
 * Vertx SNS Unsubscribe Confirmation Event
 *
 * @author Brad Behnke
 */
public class SNSUnsubscribeConfirmEvent extends NotificationEvent<GeneralMessage> {
    public static final String ADDRESS = "sns-unsubscribe";

    public SNSUnsubscribeConfirmEvent() {
        super(null, GeneralMessage.class);
    }

    public SNSUnsubscribeConfirmEvent(GeneralMessage message) {
        super(message, GeneralMessage.class);
    }
}
