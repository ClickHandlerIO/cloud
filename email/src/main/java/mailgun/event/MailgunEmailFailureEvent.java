package mailgun.event;

import common.event.EmailNotificationEvent;
import mailgun.data.FailureMessage;

/**
 * Vertx Email Delivery Event
 *
 * @author Brad Behnke
 */
public class MailgunEmailFailureEvent extends EmailNotificationEvent<FailureMessage> {
    public static final String ADDRESS = "mailgun-email-failure";

    public MailgunEmailFailureEvent() {
        super(null, FailureMessage.class);
    }

    public MailgunEmailFailureEvent(FailureMessage message) {
        super(message, FailureMessage.class);
    }
}
