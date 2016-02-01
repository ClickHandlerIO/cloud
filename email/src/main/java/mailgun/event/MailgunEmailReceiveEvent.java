package mailgun.event;

import common.event.EmailNotificationEvent;
import mailgun.data.ReceiveMessage;

/**
 * Vertx Email Delivery Event
 *
 * @author Brad Behnke
 */
public class MailgunEmailReceiveEvent extends EmailNotificationEvent {
    public static final String ADDRESS = "mailgun-email-receive";
    private ReceiveMessage message;

    public MailgunEmailReceiveEvent() {
        super(ADDRESS);
    }

    public MailgunEmailReceiveEvent(ReceiveMessage message) {
        super(ADDRESS);
        this.message = message;
    }

    public ReceiveMessage getMessage() {
        return message;
    }

    public void setMessage(ReceiveMessage message) {
        this.message = message;
    }
}
