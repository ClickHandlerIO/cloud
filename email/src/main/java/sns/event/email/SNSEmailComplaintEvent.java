package sns.event.email;

import common.event.EmailNotificationEvent;
import sns.data.json.email.notify.EmailNotifyMessage;

/**
 * Vertx Email Complaint Event
 *
 * @author Brad Behnke
 */
public class SNSEmailComplaintEvent extends EmailNotificationEvent<EmailNotifyMessage> {
    public static final String ADDRESS = "sns-email-complaint";

    public SNSEmailComplaintEvent() {
        super(null, EmailNotifyMessage.class);
    }

    public SNSEmailComplaintEvent(EmailNotifyMessage message) {
        super(message, EmailNotifyMessage.class);
    }
}
