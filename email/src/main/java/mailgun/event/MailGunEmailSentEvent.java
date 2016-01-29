package mailgun.event;

import common.event.EmailNotificationEvent;
import entity.EmailEntity;

/**
 * Vertx event for Mailgun email sent events
 *
 * @author Brad Behnke
 */
public class MailgunEmailSentEvent extends EmailNotificationEvent {
    public static final String ADDRESS = "mg-email-sent";
    private boolean successful;
    private EmailEntity emailEntity;

    public MailgunEmailSentEvent() {
        super(ADDRESS);
    }

    public MailgunEmailSentEvent(EmailEntity emailEntity, boolean successful) {
        super(ADDRESS);
        this.emailEntity = emailEntity;
        this.successful = successful;
    }

    public EmailEntity getEmailEntity() {
        return emailEntity;
    }

    public void setEmailEntity(EmailEntity emailEntity) {
        this.emailEntity = emailEntity;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }
}
