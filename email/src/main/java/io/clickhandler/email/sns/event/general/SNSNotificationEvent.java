package io.clickhandler.email.sns.event.general;

import io.clickhandler.email.common.event.NotificationEvent;
import io.clickhandler.email.sns.data.json.general.GeneralMessage;

/**
 * Vertx Notification Event from SNS
 *
 * @author Brad Behnke
 */
public class SNSNotificationEvent extends NotificationEvent<GeneralMessage> {
    public static final String ADDRESS = "io.clickhandler.email.sns-notification";

    public SNSNotificationEvent() {
        super(null, GeneralMessage.class);
    }

    public SNSNotificationEvent(GeneralMessage message) {
        super(message, GeneralMessage.class);
    }
}
