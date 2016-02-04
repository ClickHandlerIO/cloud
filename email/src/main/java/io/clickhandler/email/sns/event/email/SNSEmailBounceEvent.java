package io.clickhandler.email.sns.event.email;

import io.clickhandler.email.common.event.EmailNotificationEvent;
import io.clickhandler.email.sns.data.json.email.notify.EmailNotifyMessage;

/**
 * Vertx Email Bounced Event
 *
 * @author Brad Behnke
 */
public class SNSEmailBounceEvent extends EmailNotificationEvent<EmailNotifyMessage> {
    public static final String ADDRESS = "io.clickhandler.email.sns-email-bounce";

    public SNSEmailBounceEvent() {
        super(null, EmailNotifyMessage.class);
    }

    public SNSEmailBounceEvent(EmailNotifyMessage message) {
        super(message, EmailNotifyMessage.class);
    }
}
