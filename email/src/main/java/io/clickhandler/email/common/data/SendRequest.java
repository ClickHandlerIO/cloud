package io.clickhandler.email.common.data;

import io.clickhandler.email.entity.EmailEntity;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 *  Abstract for request to be passed to cloud-email services.
 *  <p>
 *  Required
 *
 *  EmailEntity: Object containing email database record information. Related records for recipients and attachments
 *  must exist in database prior to passing to a email sending service.
 *
 *  Handler: Callback to allow for ASync processing.
 *  </p>
 *  @author Brad behnke
 */
public abstract class SendRequest {
    private EmailEntity emailEntity;
    private Handler<AsyncResult<EmailEntity>> completionHandler;
    private int attempts = 0;

    public SendRequest(EmailEntity emailEntity) {
        this.emailEntity = emailEntity;
    }

    public EmailEntity getEmailEntity() {
        return emailEntity;
    }

    public void setEmailEntity(EmailEntity emailEntity) {
        this.emailEntity = emailEntity;
    }

    public Handler<AsyncResult<EmailEntity>> getCompletionHandler() {
        return completionHandler;
    }

    public void setCompletionHandler(Handler<AsyncResult<EmailEntity>> completionHandler) {
        this.completionHandler = completionHandler;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public void incrementAttempts() {
        this.attempts++;
    }
}