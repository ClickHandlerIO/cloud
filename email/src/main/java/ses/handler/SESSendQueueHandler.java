package ses.handler;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.amazonaws.services.simpleemail.model.SendRawEmailResult;
import com.google.common.base.Strings;
import common.handler.EmailSendQueueHandler;
import common.handler.FileGetChunksHandler;
import common.service.FileService;
import entity.EmailAttachmentEntity;
import entity.EmailEntity;
import io.clickhandler.sql.SqlExecutor;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import rx.Observable;
import ses.config.SESConfig;
import ses.data.MimeSendRequest;
import ses.event.SESEmailSentEvent;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Sends email messages queued through Amazon SES.
 *
 * @author Brad Behnke
 */
public class SESSendQueueHandler extends EmailSendQueueHandler<MimeSendRequest> {

    private final EventBus eventBus;
    private final AmazonSimpleEmailServiceClient client;
    private final int ALLOWED_ATTEMPTS;

    public SESSendQueueHandler(SESConfig sesConfig, EventBus eventBus, SqlExecutor db, FileService fileService){
        super(db, fileService);
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
    public void receive(List<MimeSendRequest> sendRequests) {
        sendRequests.forEach(this::processRequest);
    }

    private void processRequest(MimeSendRequest request) {
        final EmailEntity emailEntity = request.getEmailEntity();
        buildMimeMessageObservable(emailEntity)
                .doOnError(throwable -> {
                    failure(request, throwable);
                    publishEvent(request.getEmailEntity(), false);
                })
                .doOnNext(message -> {
                    try {
                        request.setMimeMessage(message);
                        if (emailEntity.isAttachments()) {
                            getAttachmentEntitiesObservable(request.getEmailEntity())
                                    .doOnError(throwable -> failure(request,throwable))
                                    .doOnNext(attachmentEntities -> new FileChunksIterator(request, attachmentEntities.iterator()).start());
                        } else {
                            sendEmail(request);
                        }
                    } catch (Throwable throwable) {
                        failure(request, throwable);
                        publishEvent(request.getEmailEntity(), false);
                    }
                });
    }

    private Observable<MimeMessage> buildMimeMessageObservable(EmailEntity emailEntity) {
        ObservableFuture<MimeMessage> observableFuture = RxHelper.observableFuture();
        buildMimeMessage(emailEntity, observableFuture.toHandler());
        return observableFuture;
    }

    private void buildMimeMessage(EmailEntity emailEntity, Handler<AsyncResult<MimeMessage>> completionHandler) {
        try {
            final MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
            message.setSubject((emailEntity.getSubject() == null || emailEntity.getSubject().isEmpty()) ? "No Subject" : emailEntity.getSubject());
            message.setFrom(new InternetAddress(emailEntity.getFrom()));
            message.setReplyTo(new Address[]{new InternetAddress(emailEntity.getReplyTo())});
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailEntity.getTo()));
            message.setSentDate(new Date());
            // Text version
            final MimeBodyPart textPart = new MimeBodyPart();
            textPart.setContent(emailEntity.getTextBody(), "text/plain");
            // HTML version
            final MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(emailEntity.getHtmlBody(), "text/html");
            // Wrap Parts
            final Multipart mp = new MimeMultipart("alternative");
            mp.addBodyPart(textPart);
            mp.addBodyPart(htmlPart);
            // Set wrapper as message's content
            message.setContent(mp);
            if(completionHandler != null) {
                completionHandler.handle(Future.succeededFuture(message));
            }
        } catch (Throwable throwable) {
            if(completionHandler != null) {
                completionHandler.handle(Future.failedFuture(throwable));
            }
        }
    }

    private class FileChunksIterator implements FileGetChunksHandler {

        private Iterator<EmailAttachmentEntity> it;
        private EmailAttachmentEntity currentAttachment;
        private MimeSendRequest sendRequest;
        private Buffer buffer;

        public FileChunksIterator(MimeSendRequest sendRequest, Iterator<EmailAttachmentEntity> it) {
            this.sendRequest = sendRequest;
            this.it = it;
        }

        @Override
        public void chunkReceived(Buffer chunk) {
            this.buffer.appendBuffer(chunk);
        }

        @Override
        public void onComplete() {
            try {
                MimeMessage message = sendRequest.getMimeMessage();
                MimeMultipart content = (MimeMultipart) message.getContent();
                final MimeBodyPart attachmentPart = new MimeBodyPart();
                attachmentPart.setDescription(currentAttachment.getDescription(), "UTF-8");
                final DataSource ds = new ByteArrayDataSource(buffer.getBytes(), currentAttachment.getMimeType());
                attachmentPart.setDataHandler(new DataHandler(ds));
                attachmentPart.setHeader("Content-ID", "<" + currentAttachment.getId() + ">");
                attachmentPart.setFileName(currentAttachment.getName());
                content.addBodyPart(attachmentPart);
                message.setContent(content);
                sendRequest.setMimeMessage(message);
                if(it.hasNext()) {
                    next();
                } else {
                    sendEmail(sendRequest);
                }
            } catch (Throwable throwable) {
                failure(sendRequest, throwable);
            }
        }

        @Override
        public void onFailure(Throwable throwable) {
            failure(sendRequest, throwable);
        }

        public void next() {
            if(it.hasNext()) {
                this.buffer = Buffer.buffer();
                this.currentAttachment = it.next();
                getAttachmentFile(currentAttachment, this);
            }
        }

        public void start() {
            if(it != null) {
                next();
            }
        }
    }

    private void getAttachmentFile(EmailAttachmentEntity attachmentEntity, FileGetChunksHandler handler) {
        getFileEntityObservable(attachmentEntity.getFileId())
                .doOnError(throwable -> {
                    if (handler != null) {
                        handler.onFailure(throwable);
                    }
                })
                .doOnNext(fileEntity -> getFileService().getAsyncChunks(fileEntity, handler));
    }

    private void sendEmail(final MimeSendRequest sendRequest) {
        getRawRequestObservable(sendRequest)
                .doOnError(throwable -> failure(sendRequest, throwable))
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

    private Observable<SendRawEmailRequest> getRawRequestObservable(MimeSendRequest sendRequest){
        ObservableFuture<SendRawEmailRequest> observableFuture = RxHelper.observableFuture();
        getRawRequest(sendRequest, observableFuture.toHandler());
        return observableFuture;
    }

    private void getRawRequest(MimeSendRequest sendRequest, Handler<AsyncResult<SendRawEmailRequest>> completionHandler) {
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
        eventBus.publish(SESEmailSentEvent.ADDRESS, new SESEmailSentEvent(emailEntity, success).toJson());
    }
}
