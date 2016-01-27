package sns.event.email;

import common.event.AbstractEmailNotificationEvent;
import sns.data.json.email.notify.EmailNotifyMessage;

/**
 * Vertx Email Delivery Event
 *
 * @author Brad Behnke
 */
public class SNSEmailDeliveryEvent extends AbstractEmailNotificationEvent {
    public static final String ADDRESS = "sns-email-delivery";
    private EmailNotifyMessage message;

    public SNSEmailDeliveryEvent() {
        super(ADDRESS);
    }

    public SNSEmailDeliveryEvent(EmailNotifyMessage message) {
        super(ADDRESS);
        this.message = message;
    }

    public EmailNotifyMessage getMessage() {
        return message;
    }

    public void setMessage(EmailNotifyMessage message) {
        this.message = message;
    }
}
