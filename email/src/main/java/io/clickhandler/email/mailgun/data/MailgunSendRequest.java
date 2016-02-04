package io.clickhandler.email.mailgun.data;

import io.clickhandler.email.common.data.SendRequest;
import io.clickhandler.email.entity.EmailEntity;
import io.vertx.core.MultiMap;

/**
 * Created by admin on 1/28/16.
 */
public class MailgunSendRequest extends SendRequest {

    private MultiMap content;

    public MailgunSendRequest(EmailEntity emailEntity) {
        super(emailEntity);
    }

    public MultiMap getContent() {
        return content;
    }

    public void setContent(MultiMap content) {
        this.content = content;
    }
}
