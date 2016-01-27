package sns.event.email;

import sns.event.common.SNSEvent;
import sns.json.email.receive.EmailReceivedMessage;

/**
 * Created by admin on 1/25/16.
 */
public class EmailReceivedEvent extends SNSEvent {
    public static final String ADDRESS = "sns-email-received";
    private EmailReceivedMessage message;

    public EmailReceivedEvent() {
        super(ADDRESS);
    }

    public EmailReceivedEvent(EmailReceivedMessage message) {
        super(ADDRESS);
        this.message = message;
    }

    public EmailReceivedMessage getMessage() {
        return message;
    }

    public void setMessage(EmailReceivedMessage message) {
        this.message = message;
    }
}
