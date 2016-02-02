package mailgun.event;

import common.event.EmailNotificationEvent;
import mailgun.data.DeliveryMessage;

/**
 * Vertx Email Delivery Event
 *
 * @author Brad Behnke
 */
public class MailgunEmailDeliveryEvent extends EmailNotificationEvent<DeliveryMessage> {
    public static final String ADDRESS = "mailgun-email-delivery";

    public MailgunEmailDeliveryEvent() {
        super(null, DeliveryMessage.class);
    }

    public MailgunEmailDeliveryEvent(DeliveryMessage message) {
        super(message, DeliveryMessage.class);
    }
}
