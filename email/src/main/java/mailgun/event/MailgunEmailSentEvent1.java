package mailgun.event;

import common.data.EmailSentMessage;
import common.event.EmailNotificationEvent;
import entity.EmailEntity;

/**
 * Vertx event for Mailgun email sent events
 *
 * @author Brad Behnke
 */
public class MailgunEmailSentEvent1 extends EmailNotificationEvent<EmailSentMessage> {
    public static final String ADDRESS = "mg-email-sent";

    public MailgunEmailSentEvent1() {
        super(null, EmailSentMessage.class);
    }

    public MailgunEmailSentEvent1(EmailSentMessage message) {
        super(message, EmailSentMessage.class);
    }

    public MailgunEmailSentEvent1(EmailEntity emailEntity, boolean success) {
        super(new EmailSentMessage(emailEntity, success), EmailSentMessage.class);
    }
}
