package sns.event.email;

import common.event.EmailNotificationEvent;
import sns.data.json.email.notify.EmailNotifyMessage;

/**
 * Vertx Email Delivery Event
 *
 * @author Brad Behnke
 */
public class SNSEmailDeliveryEvent extends EmailNotificationEvent<EmailNotifyMessage> {
    public static final String ADDRESS = "sns-email-delivery";

    public SNSEmailDeliveryEvent() {
        super(null, EmailNotifyMessage.class);
    }

    public SNSEmailDeliveryEvent(EmailNotifyMessage message) {
        super(message, EmailNotifyMessage.class);
    }
}
