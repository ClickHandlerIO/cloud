package io.clickhandler.email.sns.event.email;

import io.clickhandler.email.common.event.EmailNotificationEvent;
import io.clickhandler.email.sns.data.json.email.notify.EmailNotifyMessage;

/**
 * Vertx Email Delivery Event
 *
 * @author Brad Behnke
 */
public class SNSEmailDeliveryEvent extends EmailNotificationEvent<EmailNotifyMessage> {
    public static final String ADDRESS = "io.clickhandler.email.sns-email-delivery";

    public SNSEmailDeliveryEvent() {
        super(null, EmailNotifyMessage.class);
    }

    public SNSEmailDeliveryEvent(EmailNotifyMessage message) {
        super(message, EmailNotifyMessage.class);
    }
}
