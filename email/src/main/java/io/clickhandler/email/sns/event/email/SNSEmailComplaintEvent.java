package io.clickhandler.email.sns.event.email;

import io.clickhandler.email.common.event.EmailNotificationEvent;
import io.clickhandler.email.sns.data.json.email.notify.EmailNotifyMessage;

/**
 * Vertx Email Complaint Event
 *
 * @author Brad Behnke
 */
public class SNSEmailComplaintEvent extends EmailNotificationEvent<EmailNotifyMessage> {
    public static final String ADDRESS = "io.clickhandler.email.sns-email-complaint";

    public SNSEmailComplaintEvent() {
        super(null, EmailNotifyMessage.class);
    }

    public SNSEmailComplaintEvent(EmailNotifyMessage message) {
        super(message, EmailNotifyMessage.class);
    }
}
