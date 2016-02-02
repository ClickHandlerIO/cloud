package common.data;

import entity.EmailEntity;

/**
 * Created by admin on 2/1/16.
 */
public class EmailSentMessage {
    private boolean successful;
    private EmailEntity emailEntity;

    public EmailSentMessage(EmailEntity emailEntity, boolean successful) {
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
