package mailgun.event;

import common.event.EmailNotificationEvent;
import mailgun.data.ReceiveMessage;


/**
 * Vertx Email Delivery Event
 *
 * @author Brad Behnke
 */
public class MailgunEmailReceiveEvent extends EmailNotificationEvent<ReceiveMessage> {
    public static final String ADDRESS = "mailgun-email-receive";

    public MailgunEmailReceiveEvent() {
        super(null, null);
    }

    public MailgunEmailReceiveEvent(ReceiveMessage message) {
        super(message, ReceiveMessage.class);
    }
}
