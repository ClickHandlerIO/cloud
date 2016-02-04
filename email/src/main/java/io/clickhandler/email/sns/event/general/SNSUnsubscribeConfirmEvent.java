package io.clickhandler.email.sns.event.general;

import io.clickhandler.email.common.event.NotificationEvent;
import io.clickhandler.email.sns.data.json.general.GeneralMessage;

/**
 * Vertx SNS Unsubscribe Confirmation Event
 *
 * @author Brad Behnke
 */
public class SNSUnsubscribeConfirmEvent extends NotificationEvent<GeneralMessage> {
    public static final String ADDRESS = "io.clickhandler.email.sns-unsubscribe";

    public SNSUnsubscribeConfirmEvent() {
        super(null, GeneralMessage.class);
    }

    public SNSUnsubscribeConfirmEvent(GeneralMessage message) {
        super(message, GeneralMessage.class);
    }
}
