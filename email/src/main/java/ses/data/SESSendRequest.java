package ses.data;

import common.data.SendRequest;
import entity.EmailEntity;

import javax.mail.internet.MimeMessage;

/**
 *  Request wrapper object for sending an email through ses email service.
 *
 *  @author Brad Behnke
 */
public class SESSendRequest extends SendRequest {

    private MimeMessage mimeMessage;
    private int attempts = 0;

    public SESSendRequest(EmailEntity emailEntity) {
        super(emailEntity);
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
