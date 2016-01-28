package sns.event.email;

import common.event.AbstractEmailNotificationEvent;
import sns.data.json.email.receive.EmailReceivedMessage;

/**
 * Vertx Email Received Event
 *
 * @author Brad Behnke
 */
public class SNSEmailReceivedEvent extends AbstractEmailNotificationEvent {
    public static final String ADDRESS = "sns-email-received";
    private EmailReceivedMessage message;

    public SNSEmailReceivedEvent() {
        super(ADDRESS);
    }

    public SNSEmailReceivedEvent(EmailReceivedMessage message) {
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