package common;

import entity.EmailEntity;

/**
 * Created by admin on 1/26/16.
 */
public abstract class EmailSendEvent {
    private boolean successful;
    private EmailEntity emailEntity;

    public EmailSendEvent(EmailEntity emailEntity, boolean successful) {
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
