package mailgun.event;

import common.event.EmailNotificationEvent;
import mailgun.data.BounceMessage;
import mailgun.data.FailureMessage;

/**
 * Vertx Email Delivery Event
 *
 * @author Brad Behnke
 */
public class MailgunEmailFailureEvent extends EmailNotificationEvent {
    public static final String ADDRESS = "mailgun-email-failure";
    private FailureMessage message;

    public MailgunEmailFailureEvent() {
        super(ADDRESS);
    }

    public MailgunEmailFailureEvent(FailureMessage message) {
        super(ADDRESS);
        this.message = message;
    }

    public FailureMessage getMessage() {
        return message;
    }

    public void setMessage(FailureMessage message) {
        this.message = message;
    }
}
