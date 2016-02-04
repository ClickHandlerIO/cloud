package sns.event.email;

import common.event.EmailNotificationEvent;
import sns.data.json.email.receive.EmailReceivedMessage;

/**
 * Vertx Email Received Event
 *
 * @author Brad Behnke
 */
public class SNSEmailReceivedEvent extends EmailNotificationEvent<EmailReceivedMessage> {
    public static final String ADDRESS = "sns-email-received";

    public SNSEmailReceivedEvent() {
        super(null, EmailReceivedMessage.class);
    }

    public SNSEmailReceivedEvent(EmailReceivedMessage message) {
        super(message, EmailReceivedMessage.class);
    }
}
