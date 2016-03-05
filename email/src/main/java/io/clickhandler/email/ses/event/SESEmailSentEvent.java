package io.clickhandler.email.ses.event;

import io.clickhandler.email.common.data.EmailSentMessage;
import io.clickhandler.email.common.event.EmailNotificationEvent;
import io.clickhandler.cloud.model.EmailEntity;

/**
 *  Vertx event for email send notification.
 *
 *  @author Brad Behnke
 */
public class SESEmailSentEvent extends EmailNotificationEvent<EmailSentMessage> {
    public static final String ADDRESS = "io.clickhandler.email.sns-email-sent";

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
