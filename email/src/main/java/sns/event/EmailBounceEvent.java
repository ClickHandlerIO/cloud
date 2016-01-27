package sns.event;

import sns.json.email.notify.EmailNotifyMessage;

/**
 * Created by admin on 1/25/16.
 */
public class EmailBounceEvent extends SNSEvent {
    public static final String ADDRESS = "sns-email-bounce";
    private EmailNotifyMessage message;

    public EmailBounceEvent() {
        super(ADDRESS);
    }

    public EmailBounceEvent(EmailNotifyMessage message) {
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
