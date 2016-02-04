package ses.event;

import common.data.EmailSentMessage;
import common.event.EmailNotificationEvent;
import entity.EmailEntity;

/**
 *  Vertx event for email send notification.
 *
 *  @author Brad Behnke
 */
public class SESEmailSentEvent extends EmailNotificationEvent<EmailSentMessage> {
    public static final String ADDRESS = "sns-email-sent";

    public SESEmailSentEvent() {
        super(null, EmailSentMessage.class);
    }

    public SESEmailSentEvent(EmailSentMessage message) {
        super(message, EmailSentMessage.class);
    }

    public SESEmailSentEvent(EmailEntity emailEntity, boolean success) {
        super(new EmailSentMessage(emailEntity, success), EmailSentMessage.class);
    }
}
