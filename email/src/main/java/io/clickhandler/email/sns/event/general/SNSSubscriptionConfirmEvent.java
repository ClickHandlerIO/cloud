package io.clickhandler.email.sns.event.general;

import io.clickhandler.email.common.event.NotificationEvent;
import io.clickhandler.email.sns.data.json.general.GeneralMessage;

/**
 * Vertx SNS Subcription Confirmation Event
 *
 * @author Brad Behnke
 */
public class SNSSubscriptionConfirmEvent extends NotificationEvent<GeneralMessage> {
    public static final String ADDRESS = "io.clickhandler.email.sns-subscribe";

    public SNSSubscriptionConfirmEvent() {
        super(null, GeneralMessage.class);
    }

    public SNSSubscriptionConfirmEvent(GeneralMessage message) {
        super(message, GeneralMessage.class);
    }
}
