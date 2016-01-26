package ses.event;

import entity.EmailEntity;

/**
 * Created by admin on 1/26/16.
 */
public class EmailSendEvent extends SESEvent {
    private boolean success;
    private EmailEntity emailEntity;

    public EmailSendEvent(boolean success,EmailEntity emailEntity) {
        this.success = success;
        this.emailEntity = emailEntity;
    }

    public EmailEntity getEmailEntity() {
        return emailEntity;
    }

    public void setEmailEntity(EmailEntity emailEntity) {
        this.emailEntity = emailEntity;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
