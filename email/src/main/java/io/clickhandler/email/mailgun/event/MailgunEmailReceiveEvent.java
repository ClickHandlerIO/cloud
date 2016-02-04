package io.clickhandler.email.mailgun.event;

import io.clickhandler.email.common.event.EmailNotificationEvent;
import io.clickhandler.email.mailgun.data.ReceiveMessage;


/**
 * Vertx Email Delivery Event
 *
 * @author Brad Behnke
 */
public class MailgunEmailReceiveEvent extends EmailNotificationEvent<ReceiveMessage> {
    public static final String ADDRESS = "io.clickhandler.email.mailgun-email-receive";

    public MailgunEmailReceiveEvent() {
        super(null, null);
    }

    public MailgunEmailReceiveEvent(ReceiveMessage message) {
        super(message, ReceiveMessage.class);
    }
}
