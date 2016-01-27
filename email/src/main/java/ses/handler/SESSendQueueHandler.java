package ses.handler;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.amazonaws.services.simpleemail.model.SendRawEmailResult;
import com.google.common.base.Strings;
import data.schema.Tables;
import entity.EmailEntity;
import entity.EmailRecipientEntity;
import entity.RecipientStatus;
import io.clickhandler.queue.QueueHandler;
import io.clickhandler.sql.db.*;
import io.vertx.rxjava.core.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ses.config.SESConfig;
import ses.data.SESSendRequest;
import ses.event.SESEmailSentEvent;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.List;

/**
 * Sends email messages queued through Amazon SES.
 *
 * @author Brad Behnke
 */
public class SESSendQueueHandler implements QueueHandler<SESSendRequest>, Tables {

    private final static Logger LOG = LoggerFactory.getLogger(SESSendQueueHandler.class);
    private final EventBus eventBus;
    private final SqlDatabase db;
    private final AmazonSimpleEmailServiceClient client;
    private final int ALLOWED_ATTEMPTS;

    public SESSendQueueHandler(EventBus eventBus, SqlDatabase db){
        this.eventBus = eventBus;
        this.db = db;
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
    public void receive(List<SESSendRequest> sendRequests) {
        sendRequests.forEach(this::sendEmail);
    }

    private void sendEmail(final SESSendRequest sendRequest) {
        try {
            sendRequest.incrementAttempts();
            SendRawEmailResult result = client.sendRawEmail(buildEmailRequest(sendRequest));
            // send failed
            if (result == null || result.getMessageId() == null || result.getMessageId().isEmpty()) {
                if(sendRequest.getAttempts() < ALLOWED_ATTEMPTS) {
                    sendEmail(sendRequest);
                } else {
                    updateRecords(sendRequest.getEmailEntity(), null);
                    sendRequest.getSendHandler().onFailure(new Exception("Failed to send."));
                    publishEvent(sendRequest.getEmailEntity(), false);
                }
            }
            // send success
            else {
                EmailEntity emailEntity = updateRecords(sendRequest.getEmailEntity(), result.getMessageId());
                sendRequest.getSendHandler().onSuccess(emailEntity);
                publishEvent(emailEntity, true);
            }
        } catch (Exception e) {
            // send or record update failed
            if(sendRequest.getAttempts() < ALLOWED_ATTEMPTS) {
                sendEmail(sendRequest);
            } else {
                try {
                    updateRecords(sendRequest.getEmailEntity(), null);
                    sendRequest.getSendHandler().onFailure(e);
                } catch (Exception e1) {
                    // record update failed
                    sendRequest.getSendHandler().onFailure(e1);
                }
                publishEvent(sendRequest.getEmailEntity(), false);
            }
        }
    }

    private SendRawEmailRequest buildEmailRequest(SESSendRequest sendRequest) throws Exception{
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        sendRequest.getMimeMessage().writeTo(outputStream);
        RawMessage rawMessage = new RawMessage(ByteBuffer.wrap(outputStream.toByteArray()));
        return new SendRawEmailRequest(rawMessage);
    }

    private EmailEntity updateRecords(final EmailEntity emailEntity, final String sesMessageId) throws Exception {
        boolean success = sesMessageId != null && !sesMessageId.isEmpty();
        // update email record
        if(success) {
            emailEntity.setMessageId(sesMessageId);
            db.writeObservable(session -> new SqlResult<>(session.update(emailEntity) == 1, emailEntity)).subscribe(emailEntitySqlResult -> {
                if(!emailEntitySqlResult.isSuccess()) {
                    LOG.error("Email failed to update on send.");
                }
            });
        }

        // update recipient records
        // get recipients
        final SqlHelper sqlHelper = new SqlHelper();
        db.readObservable(session -> session.select(EMAIL_RECIPIENT.fields())
                .where(EMAIL_RECIPIENT.EMAIL_ID.eq(emailEntity.getId()))
                .fetch().into(EMAIL_RECIPIENT).into(EmailRecipientEntity.class)).subscribe(emailRecipientEntities -> {
            sqlHelper.setRecipientEntities(emailRecipientEntities);
            sqlHelper.notify();
        });
        sqlHelper.wait();
        if(sqlHelper.getRecipientEntities() == null || sqlHelper.getRecipientEntities().isEmpty()) {
            throw new Exception("Email " + (success ? "sent " : "failed ") + "and recipient record(s) update failed.");
        }
        // update recipients
        for(final EmailRecipientEntity recipientEntity:sqlHelper.getRecipientEntities()) {
            if(success) {
                recipientEntity.setStatus(RecipientStatus.SENT);
                recipientEntity.setSent(new Date());
            } else {
                recipientEntity.setStatus(RecipientStatus.FAILED);
                recipientEntity.setFailed(new Date());
            }
            db.writeObservable(session -> new SqlResult<>(session.update(recipientEntity) == 1, recipientEntity)).subscribe(recipientEntitySqlResult -> {
                if (!recipientEntitySqlResult.isSuccess()) {
                    LOG.error("EmailRecipient failed to update on send.");
                }
            });
        }
        return emailEntity;
    }

    private void publishEvent(EmailEntity emailEntity, boolean success) {
        eventBus.publish(SESEmailSentEvent.ADDRESS, new SESEmailSentEvent(emailEntity, success));
    }

    class SqlHelper {
        List<EmailRecipientEntity> recipientEntities;

        public SqlHelper() {
        }

        public List<EmailRecipientEntity> getRecipientEntities() {
            return recipientEntities;
        }

        public void setRecipientEntities(List<EmailRecipientEntity> recipientEntities) {
            this.recipientEntities = recipientEntities;
        }
    }
}
