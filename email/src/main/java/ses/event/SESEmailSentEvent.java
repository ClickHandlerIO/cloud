package ses.event;

import common.event.AbstractEmailNotificationEvent;
import entity.EmailEntity;

/**
 * Created by admin on 1/26/16.
 */
public class SESEmailSentEvent extends AbstractEmailNotificationEvent {
    public static final String ADDRESS = "sns-email-sent";
    private boolean successful;
    private EmailEntity emailEntity;

    public SESEmailSentEvent() {
        super(ADDRESS);
    }

    public SESEmailSentEvent(EmailEntity emailEntity, boolean successful) {
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
