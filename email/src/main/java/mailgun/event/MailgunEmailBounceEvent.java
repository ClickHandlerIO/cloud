package mailgun.event;

import common.event.EmailNotificationEvent;
import mailgun.data.BounceMessage;

/**
 * Vertx Email Delivery Event
 *
 * @author Brad Behnke
 */
public class MailgunEmailBounceEvent extends EmailNotificationEvent {
    public static final String ADDRESS = "mailgun-email-bounce";
    private BounceMessage message;

    public MailgunEmailBounceEvent() {
        super(ADDRESS);
    }

    public MailgunEmailBounceEvent(BounceMessage message) {
        super(ADDRESS);
        this.message = message;
    }

    public BounceMessage getMessage() {
        return message;
    }

    public void setMessage(BounceMessage message) {
        this.message = message;
    }
}
