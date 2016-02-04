package io.clickhandler.email.mailgun.event;

import io.clickhandler.email.common.event.EmailNotificationEvent;
import io.clickhandler.email.mailgun.data.BounceMessage;

/**
 * Vertx Email Delivery Event
 *
 * @author Brad Behnke
 */
public class MailgunEmailBounceEvent extends EmailNotificationEvent<BounceMessage> {
    public static final String ADDRESS = "io.clickhandler.email.mailgun-email-bounce";

    public MailgunEmailBounceEvent() {
        super(null, BounceMessage.class);
    }

    public MailgunEmailBounceEvent(BounceMessage message) {
        super(message, BounceMessage.class);
    }
}
