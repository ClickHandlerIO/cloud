package sns.event.email;

import common.event.AbstractEmailNotificationEvent;
import sns.data.json.email.notify.EmailNotifyMessage;

/**
 * Vertx Email Bounced Event
 *
 * @author Brad Behnke
 */
public class SNSEmailBounceEvent extends AbstractEmailNotificationEvent {
    public static final String ADDRESS = "sns-email-bounce";
    private EmailNotifyMessage message;

    public SNSEmailBounceEvent() {
        super(ADDRESS);
    }

    public SNSEmailBounceEvent(EmailNotifyMessage message) {
        super(ADDRESS);
        this.message = message;
    }

    public EmailNotifyMessage getMessage() {
        return message;
    }

    public void setMessage(EmailNotifyMessage message) {
        this.message = message;
    }
}
