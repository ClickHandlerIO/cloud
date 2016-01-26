package ses.data;

import common.AbstractSendRequest;
import common.SendHandler;
import entity.EmailEntity;

import javax.mail.internet.MimeMessage;

/**
 * Created by admin on 1/26/16.
 */
public class SESSendRequest extends AbstractSendRequest{

    private MimeMessage mimeMessage;
    private int attempts = 0;

    public SESSendRequest(EmailEntity emailEntity, SendHandler sendHandler) {
        super(emailEntity, sendHandler);
    }

    public MimeMessage getMimeMessage() {
        return mimeMessage;
    }

    public void setMimeMessage(MimeMessage mimeMessage) {
        this.mimeMessage = mimeMessage;
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
