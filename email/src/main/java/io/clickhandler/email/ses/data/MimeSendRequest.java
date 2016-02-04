package io.clickhandler.email.ses.data;

import io.clickhandler.email.common.data.SendRequest;
import io.clickhandler.email.entity.EmailEntity;

import javax.mail.internet.MimeMessage;

/**
 *  Request wrapper object for sending an email through io.clickhandler.email.ses email service.
 *
 *  @author Brad Behnke
 */
public class MimeSendRequest extends SendRequest {

    private MimeMessage mimeMessage;

    public MimeSendRequest(EmailEntity emailEntity) {
        super(emailEntity);
    }

    public MimeMessage getMimeMessage() {
        return mimeMessage;
    }

    public void setMimeMessage(MimeMessage mimeMessage) {
        this.mimeMessage = mimeMessage;
    }
}
