package common;

import entity.EmailEntity;

/**
 * Created by admin on 1/26/16.
 */
public abstract class AbstractSendRequest {
    private EmailEntity emailEntity;
    private SendHandler sendHandler;

    public AbstractSendRequest(EmailEntity emailEntity, SendHandler sendHandler) {
        this.emailEntity = emailEntity;
        this.sendHandler = sendHandler;
    }

    public EmailEntity getEmailEntity() {
        return emailEntity;
    }

    public void setEmailEntity(EmailEntity emailEntity) {
        this.emailEntity = emailEntity;
    }

    public SendHandler getSendHandler() {
        return sendHandler;
    }

    public void setSendHandler(SendHandler sendHandler) {
        this.sendHandler = sendHandler;
    }
}
