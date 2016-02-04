package mailgun.event;

import common.data.EmailSentMessage;
import common.event.EmailNotificationEvent;
import entity.EmailEntity;

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