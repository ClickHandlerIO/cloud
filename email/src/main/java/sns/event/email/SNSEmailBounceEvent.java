package sns.event.email;

import common.event.EmailNotificationEvent;
import sns.data.json.email.notify.EmailNotifyMessage;

/**
 * Vertx Email Bounced Event
 *
 * @author Brad Behnke
 */
public class SNSEmailBounceEvent extends EmailNotificationEvent<EmailNotifyMessage> {
    public static final String ADDRESS = "sns-email-bounce";

    public SNSEmailBounceEvent() {
        super(null, EmailNotifyMessage.class);
    }

    public SNSEmailBounceEvent(EmailNotifyMessage message) {
        super(message, EmailNotifyMessage.class);
    }
}
