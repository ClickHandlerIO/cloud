package mailgun.event;

import common.event.EmailNotificationEvent;
import mailgun.data.DeliveryMessage;

/**
 * Vertx Email Delivery Event
 *
 * @author Brad Behnke
 */
public class MailgunEmailDeliveryEvent extends EmailNotificationEvent {
    public static final String ADDRESS = "mailgun-email-delivery";
    private DeliveryMessage message;

    public MailgunEmailDeliveryEvent() {
        super(ADDRESS);
    }

    public MailgunEmailDeliveryEvent(DeliveryMessage message) {
        super(ADDRESS);
        this.message = message;
    }

    public DeliveryMessage getMessage() {
        return message;
    }

    public void setMessage(DeliveryMessage message) {
        this.message = message;
    }
}
