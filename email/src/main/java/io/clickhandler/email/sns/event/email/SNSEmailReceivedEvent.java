package io.clickhandler.email.sns.event.email;

import io.clickhandler.email.common.event.EmailNotificationEvent;
import io.clickhandler.email.sns.data.json.email.receive.EmailReceivedMessage;

/**
 * Vertx Email Received Event
 *
 * @author Brad Behnke
 */
public class SNSEmailReceivedEvent extends EmailNotificationEvent<EmailReceivedMessage> {
    public static final String ADDRESS = "io.clickhandler.email.sns-email-received";

    public SNSEmailReceivedEvent() {
        super(null, EmailReceivedMessage.class);
    }

    public SNSEmailReceivedEvent(EmailReceivedMessage message) {
        super(message, EmailReceivedMessage.class);
    }
}
