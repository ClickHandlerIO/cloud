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
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.core.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
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
    private final SqlExecutor db;
    private final AmazonSimpleEmailServiceClient client;
    private final int ALLOWED_ATTEMPTS;

    public SESSendQueueHandler(SESConfig sesConfig, EventBus eventBus, SqlExecutor db){
        this.eventBus = eventBus;
        this.db = db;
        final BasicAWSCredentials AWSCredentials = new BasicAWSCredentials(
                Strings.nullToEmpty(sesConfig.getAwsAccessKey()),
                Strings.nullToEmpty(sesConfig.getAwsSecretKey())
        );
        this.client = new AmazonSimpleEmailServiceClient(AWSCredentials);
        this.client.setRegion(sesConfig.getAwsRegion());
        ALLOWED_ATTEMPTS = sesConfig.getSendRetryMax();
    }

    public void shutdown(){
        client.shutdown();
    }

    @Override
    public void receive(List<SESSendRequest> sendRequests) {
        sendRequests.forEach(this::sendEmail);
    }

    private void sendEmail(final SESSendRequest sendRequest) {
        getRawRequestObservable(sendRequest)
                .doOnError(throwable -> {
                    if(sendRequest.getCompletionHandler() != null) {
                        sendRequest.getCompletionHandler().handle(Future.failedFuture(throwable));
                    }
                })
                .doOnNext(sendRawEmailRequest -> {
                    sendRequest.incrementAttempts();
                    SendRawEmailResult result = client.sendRawEmail(sendRawEmailRequest);
                    if(result == null || result.getMessageId() == null || result.getMessageId().isEmpty()) {
                        if(sendRequest.getAttempts() < ALLOWED_ATTEMPTS) {
                            sendEmail(sendRequest);
                        } else {
                            updateRecords(sendRequest.getEmailEntity());
                            if(sendRequest.getCompletionHandler() != null) {
                                sendRequest.getCompletionHandler().handle(Future.failedFuture(new Exception("Failed to Send Email Using " + ALLOWED_ATTEMPTS + " Attempts.")));
                            }
                            publishEvent(sendRequest.getEmailEntity(), false);
                        }
                    } else {
                        EmailEntity emailEntity = sendRequest.getEmailEntity();
                        emailEntity.setMessageId(result.getMessageId());
                        updateRecords(emailEntity);
                        if(sendRequest.getCompletionHandler() != null) {
                            sendRequest.getCompletionHandler().handle(Future.succeededFuture(emailEntity));
                        }
                        publishEvent(emailEntity, true);
                    }
                });
    }

    private Observable<SendRawEmailRequest> getRawRequestObservable(SESSendRequest sendRequest){
        ObservableFuture<SendRawEmailRequest> observableFuture = RxHelper.observableFuture();
        getRawRequest(sendRequest, observableFuture.toHandler());
        return observableFuture;
    }

    private void getRawRequest(SESSendRequest sendRequest, Handler<AsyncResult<SendRawEmailRequest>> completionHandler) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            sendRequest.getMimeMessage().writeTo(outputStream);
            RawMessage rawMessage = new RawMessage(ByteBuffer.wrap(outputStream.toByteArray()));
            if(completionHandler != null) {
                completionHandler.handle(Future.succeededFuture(new SendRawEmailRequest(rawMessage)));
            }
        } catch (Throwable e) {
            if(completionHandler != null) {
                completionHandler.handle(Future.failedFuture(e));
            }
        }
    }

    private void updateRecords(EmailEntity emailEntity) {
        final boolean success = emailEntity.getMessageId() != null && !emailEntity.getMessageId().isEmpty();
        updateEmailEntityObservable(emailEntity)
                .doOnError(throwable -> LOG.error(throwable.getMessage()))
                .doOnNext(emailEntity1 -> updateEmailRecipientEntitiesObservable(emailEntity1.getId(), success)
                        .doOnError(throwable -> LOG.error(throwable.getMessage())));
    }

    private Observable<EmailEntity> updateEmailEntityObservable(EmailEntity emailEntity) {
        ObservableFuture<EmailEntity> observableFuture = RxHelper.observableFuture();
        updateEmailEntity(emailEntity, observableFuture.toHandler());
        return observableFuture;
    }

    private void updateEmailEntity(EmailEntity emailEntity, Handler<AsyncResult<EmailEntity>> completionHandler) {
        db.writeObservable(session -> new SqlResult<>((session.update(emailEntity) == 1), emailEntity))
                .doOnError(throwable -> {
                    if(completionHandler != null) {
                        completionHandler.handle(Future.failedFuture(throwable));
                    }
                })
                .doOnNext(emailEntitySqlResult -> {
                    if(completionHandler != null) {
                        if(emailEntitySqlResult.isSuccess()) {
                            completionHandler.handle(Future.succeededFuture(emailEntitySqlResult.get()));
                        } else {
                            completionHandler.handle(Future.failedFuture(new Exception("Email Entity Update Failed.")));
                        }
                    }
                });
    }

    private Observable<Integer> updateEmailRecipientEntitiesObservable(String emailId, boolean success) {
        ObservableFuture<Integer> observableFuture = RxHelper.observableFuture();
        updateEmailRecipientEntities(emailId, success, observableFuture.toHandler());
        return observableFuture;
    }

    private void updateEmailRecipientEntities(String emailId, boolean success, Handler<AsyncResult<Integer>> completionHandler) {
        db.readObservable(session ->
                session.select(EMAIL_RECIPIENT.fields()).from(EMAIL_RECIPIENT)
                        .where(EMAIL_RECIPIENT.EMAIL_ID.eq(emailId))
                        .fetch().into(EMAIL_RECIPIENT).into(EmailRecipientEntity.class))
                .doOnError(throwable -> {
                    if(completionHandler != null) {
                        completionHandler.handle(Future.failedFuture(throwable));
                    }
                })
                .doOnNext(emailRecipientEntities -> {
                   for(EmailRecipientEntity recipientEntity:emailRecipientEntities) {
                       if(success) {
                           recipientEntity.setStatus(RecipientStatus.SENT);
                           recipientEntity.setSent(new Date());
                       } else {
                           recipientEntity.setStatus(RecipientStatus.FAILED);
                           recipientEntity.setFailed(new Date());
                       }
                       db.writeObservable(session -> {
                                    Integer result = session.update(recipientEntity);
                                    return new SqlResult<>(result == 1, result);
                                })
                               .doOnError(throwable -> {
                                   if(completionHandler != null) {
                                       completionHandler.handle(Future.failedFuture(throwable));
                                   }
                               })
                               .doOnNext(integerSqlResult -> {
                                   if(completionHandler != null) {
                                       if (integerSqlResult.isSuccess()) {
                                            completionHandler.handle(Future.succeededFuture(integerSqlResult.get()));
                                       } else {
                                            completionHandler.handle(Future.failedFuture(new Exception("Email Recipient Entity Update Failed.")));
                                       }
                                   }
                               });
                   }
                });
    }

    private void publishEvent(EmailEntity emailEntity, boolean success) {
        eventBus.publish(SESEmailSentEvent.ADDRESS, new SESEmailSentEvent(emailEntity, success));
    }
}
