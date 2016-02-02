package sns.event.general;

import common.event.NotificationEvent;
import sns.data.json.general.GeneralMessage;

/**
 * Vertx Notification Event from SNS
 *
 * @author Brad Behnke
 */
public class SNSNotificationEvent extends NotificationEvent<GeneralMessage> {
    public static final String ADDRESS = "sns-notification";

    public SNSNotificationEvent() {
        super(null, GeneralMessage.class);
    }

    public SNSNotificationEvent(GeneralMessage message) {
        super(message, GeneralMessage.class);
    }
}
