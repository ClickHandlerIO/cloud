package io.clickhandler.email.mailgun.event;

import io.clickhandler.email.common.data.EmailSentMessage;
import io.clickhandler.email.common.event.EmailNotificationEvent;
import io.clickhandler.email.entity.EmailEntity;

/**
 * Vertx event for Mailgun email sent events
 *
 * @author Brad Behnke
 */
public class MailgunEmailSentEvent extends EmailNotificationEvent<EmailSentMessage> {
    public static final String ADDRESS = "mg-email-sent";

    public MailgunEmailSentEvent() {
        super(null, EmailSentMessage.class);
    }

    public MailgunEmailSentEvent(EmailSentMessage message) {
        super(message, EmailSentMessage.class);
    }

    public MailgunEmailSentEvent(EmailEntity emailEntity, boolean success) {
        super(new EmailSentMessage(emailEntity, success), EmailSentMessage.class);
    }
}
