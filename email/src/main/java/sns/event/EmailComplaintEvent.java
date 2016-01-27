package sns.event;

import sns.json.email.notify.EmailNotifyMessage;

/**
 * Created by admin on 1/25/16.
 */
public class EmailComplaintEvent extends SNSEvent {
    public static final String ADDRESS = "sns-email-complaint";
    private EmailNotifyMessage message;

    public EmailComplaintEvent() {
        super(ADDRESS);
    }

    public EmailComplaintEvent(EmailNotifyMessage message) {
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
