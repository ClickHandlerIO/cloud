package ses.handler;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.amazonaws.services.simpleemail.model.SendRawEmailResult;
import com.google.common.base.Strings;
import common.handler.EmailSendQueueHandler;
import entity.EmailEntity;
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
import java.util.List;

/**
 * Sends email messages queued through Amazon SES.
 *
 * @author Brad Behnke
 */
public class SESSendQueueHandler extends EmailSendQueueHandler<SESSendRequest> {

    private final static Logger LOG = LoggerFactory.getLogger(SESSendQueueHandler.class);
    private final EventBus eventBus;
    private final AmazonSimpleEmailServiceClient client;
    private final int ALLOWED_ATTEMPTS;

    public SESSendQueueHandler(SESConfig sesConfig, EventBus eventBus, SqlExecutor db){
        super(db);
        this.eventBus = eventBus;
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
                    failure(sendRequest, throwable);
                })
                .doOnNext(sendRawEmailRequest -> {
                    sendRequest.incrementAttempts();
                    SendRawEmailResult result = client.sendRawEmail(sendRawEmailRequest);
                    if(result == null || result.getMessageId() == null || result.getMessageId().isEmpty()) {
                        if(sendRequest.getAttempts() < ALLOWED_ATTEMPTS) {
                            sendEmail(sendRequest);
                        } else {
                            updateRecords(sendRequest.getEmailEntity());
                            publishEvent(sendRequest.getEmailEntity(), false);
                            failure(sendRequest, new Exception("Failed to Send Email Using " + ALLOWED_ATTEMPTS + " Attempts."));
                        }
                    } else {
                        EmailEntity emailEntity = sendRequest.getEmailEntity();
                        emailEntity.setMessageId(result.getMessageId());
                        updateRecords(emailEntity);
                        publishEvent(emailEntity, true);
                        success(sendRequest,emailEntity);
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

    private void publishEvent(EmailEntity emailEntity, boolean success) {
        eventBus.publish(SESEmailSentEvent.ADDRESS, new SESEmailSentEvent(emailEntity, success));
    }
}
