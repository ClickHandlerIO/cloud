package common;

import entity.EmailEntity;
import event.AbstractVertxEvent;

/**
 * Created by admin on 1/26/16.
 */
public abstract class EmailSendEvent extends AbstractVertxEvent{
    private boolean successful;
    private EmailEntity emailEntity;

    public EmailSendEvent(String eventAddress) {
        super(eventAddress);
    }

    public EmailSendEvent(String eventAddress, EmailEntity emailEntity, boolean successful) {
        super(eventAddress);
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
