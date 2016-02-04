package io.clickhandler.email.mailgun.event;

import io.clickhandler.email.common.event.EmailNotificationEvent;
import io.clickhandler.email.mailgun.data.DeliveryMessage;

/**
 * Vertx Email Delivery Event
 *
 * @author Brad Behnke
 */
public class MailgunEmailDeliveryEvent extends EmailNotificationEvent<DeliveryMessage> {
    public static final String ADDRESS = "io.clickhandler.email.mailgun-email-delivery";

    public MailgunEmailDeliveryEvent() {
        super(null, DeliveryMessage.class);
    }

    public MailgunEmailDeliveryEvent(DeliveryMessage message) {
        super(message, DeliveryMessage.class);
    }
}
