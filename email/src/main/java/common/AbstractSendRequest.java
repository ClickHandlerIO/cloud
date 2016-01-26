package common;

import com.sun.istack.internal.NotNull;
import entity.EmailEntity;

/**
 * Created by Brad Behnke on 1/26/16.
 */
public abstract class AbstractSendRequest {
    private EmailEntity emailEntity;
    private SendHandler sendHandler;

    public AbstractSendRequest(@NotNull EmailEntity emailEntity, @NotNull SendHandler sendHandler) {
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
