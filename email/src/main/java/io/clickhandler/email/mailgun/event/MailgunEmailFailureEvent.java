package io.clickhandler.email.mailgun.event;

import io.clickhandler.email.common.event.EmailNotificationEvent;
import io.clickhandler.email.mailgun.data.FailureMessage;

/**
 * Vertx Email Delivery Event
 *
 * @author Brad Behnke
 */
public class MailgunEmailFailureEvent extends EmailNotificationEvent<FailureMessage> {
    public static final String ADDRESS = "io.clickhandler.email.mailgun-email-failure";

    public MailgunEmailFailureEvent() {
        super(null, FailureMessage.class);
    }

    public MailgunEmailFailureEvent(FailureMessage message) {
        super(message, FailureMessage.class);
    }
}
