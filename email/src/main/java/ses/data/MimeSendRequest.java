package ses.data;

import common.data.SendRequest;
import entity.EmailEntity;

import javax.mail.internet.MimeMessage;

/**
 *  Request wrapper object for sending an email through ses email service.
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
