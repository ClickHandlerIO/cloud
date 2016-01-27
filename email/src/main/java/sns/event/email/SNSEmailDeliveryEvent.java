package sns.event.email;

import common.event.AbstractEmailNotificationEvent;
import sns.data.json.email.notify.EmailNotifyMessage;

/**
 * Created by admin on 1/25/16.
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
