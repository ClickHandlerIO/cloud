package mailgun.event;

import common.event.EmailNotificationEvent;
import mailgun.data.BounceMessage;

/**
 * Vertx Email Delivery Event
 *
 * @author Brad Behnke
 */
public class MailgunEmailBounceEvent extends EmailNotificationEvent<BounceMessage> {
    public static final String ADDRESS = "mailgun-email-bounce";

    public MailgunEmailBounceEvent() {
        super(null, BounceMessage.class);
    }

    public MailgunEmailBounceEvent(BounceMessage message) {
        super(message, BounceMessage.class);
    }
}
