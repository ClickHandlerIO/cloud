package ses.handler;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.google.common.base.Strings;
import data.schema.Tables;
import entity.EmailEntity;
import entity.EmailRecipientEntity;
import entity.RecipientStatus;
import io.clickhandler.queue.QueueHandler;
import io.clickhandler.sql.db.Database;
import io.clickhandler.sql.db.DatabaseSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ses.config.SESConfig;
import ses.service.SESSendService;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.List;

/**
 * Sends email messages given
 *
 * @author Brad Behnke
 */
public class SESSendQueueHandler implements QueueHandler<SESSendService.Message>, Tables {

    private final static Logger LOG = LoggerFactory.getLogger(SESSendQueueHandler.class);
    private final DatabaseSession db;
    private final AmazonSimpleEmailServiceClient client;
    private final int ALLOWED_ATTEMPTS;

    public SESSendQueueHandler(Database db){
        this.db = db.getSession();
        final BasicAWSCredentials AWSCredentials = new BasicAWSCredentials(
                Strings.nullToEmpty(SESConfig.getAwsAccessKey()),
                Strings.nullToEmpty(SESConfig.getAwsSecretKey())
        );
        this.client = new AmazonSimpleEmailServiceClient(AWSCredentials);
        this.client.setRegion(SESConfig.getAwsRegion());
        ALLOWED_ATTEMPTS = SESConfig.getSendRetryMax();
    }

    public void shutdown(){
        client.shutdown();
    }

    @Override
    public void receive(List<SESSendService.Message> messages) {
        messages.forEach(this::sendEmail);
    }

    private void sendEmail(final SESSendService.Message message) {
        try {
            message.incrementAttempts();
            updateRecords(message, client.sendRawEmail(buildEmailRequest(message)).getMessageId());
        } catch (Exception e) {
            if(message.getAttempts() < ALLOWED_ATTEMPTS) {
                sendEmail(message);
            } else {
                LOG.error("Email Failed to Send", e);
                updateRecords(message, null);
            }
        }
    }

    private SendRawEmailRequest buildEmailRequest(SESSendService.Message message) throws Exception{
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        message.getMimeMessage().writeTo(outputStream);
        RawMessage rawMessage = new RawMessage(ByteBuffer.wrap(outputStream.toByteArray()));
        return new SendRawEmailRequest(rawMessage);
    }

    private void updateRecords(SESSendService.Message message, final String sesMessageId) {
        EmailEntity emailEntity = db.getEntity(EmailEntity.class, message.getEmailId());
        if(emailEntity == null) {
            LOG.error("Failed to Update Email Record");
            return;
        }
        if(sesMessageId != null) {
            emailEntity.setMessageId(sesMessageId);
            db.update(emailEntity);
        }
        // update recipient records
        List<EmailRecipientEntity> recipientEntities = db.select(EMAIL_RECIPIENT.fields())
                .where(EMAIL_RECIPIENT.EMAIL_ID.eq(emailEntity.getId()))
                .fetch().into(EMAIL_RECIPIENT).into(EmailRecipientEntity.class);
        for(EmailRecipientEntity recipientEntity:recipientEntities) {
            if(sesMessageId == null) {
                recipientEntity.setStatus(RecipientStatus.FAILED);
                recipientEntity.setFailed(new Date());
            } else {
                recipientEntity.setStatus(RecipientStatus.SENT);
                recipientEntity.setSent(new Date());
            }
        }
    }
}
