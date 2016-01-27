package common.data;

import com.sun.istack.internal.NotNull;
import common.handler.SendHandler;
import entity.EmailEntity;

/**
 *  Abstract for request to be passed to cloud-email services.
 *  <p>
 *  Required
 *
 *  EmailEntity: Object containing email database record information. Related records for recipients and attachments
 *  must exist in database prior to passing to a email sending service.
 *
 *  SendHandler: Callback to allow for ASync processing.
 *  </p>
 *  @author Brad behnke
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
