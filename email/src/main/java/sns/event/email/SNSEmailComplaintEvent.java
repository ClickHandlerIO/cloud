package sns.event.email;

import common.event.AbstractEmailNotificationEvent;
import sns.data.json.email.notify.EmailNotifyMessage;

/**
 * Vertx Email Complaint Event
 *
 * @author Brad Behnke
 */
public class SNSEmailComplaintEvent extends AbstractEmailNotificationEvent {
    public static final String ADDRESS = "sns-email-complaint";
    private EmailNotifyMessage message;

    public SNSEmailComplaintEvent() {
        super(ADDRESS);
    }

    public SNSEmailComplaintEvent(EmailNotifyMessage message) {
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