package ses.handler;

import com.google.common.base.Preconditions;
import data.schema.Tables;
import entity.EmailAttachmentEntity;
import entity.EmailEntity;
import io.clickhandler.queue.QueueHandler;
import io.clickhandler.sql.db.SqlDatabase;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.core.eventbus.EventBus;
import rx.Observable;
import ses.data.DownloadRequest;
import ses.data.SESSendRequest;
import ses.event.SESEmailSentEvent;
import ses.service.SESAttachmentService;
import ses.service.SESSendService;

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
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Prepares email messages by building messages from db email record. Also downloads attachments for email using
 * S3Service. Once built, passes email message to be sent to SESSendService.
 *
 * @author Brad Behnke
 */
public class SESPrepQueueHandler  implements QueueHandler<SESSendRequest>, Tables {

    private final EventBus eventBus;
    private final SqlDatabase db;
    private final SESAttachmentService SESAttachmentService;
    private final SESSendService sesSendService;

    public SESPrepQueueHandler(EventBus eventBus, SqlDatabase db, SESAttachmentService SESAttachmentService, SESSendService sesSendService) {
        this.eventBus = eventBus;
        this.db = db;
        this.SESAttachmentService = SESAttachmentService;
        this.sesSendService = sesSendService;
    }

    @Override
    public void receive(List<SESSendRequest> sendRequests) {
        for (final SESSendRequest request:sendRequests) {
            try {
                final EmailEntity emailEntity = request.getEmailEntity();
                final MimeMessage message = buildMimeMessage(emailEntity);
                if(emailEntity.isAttachments()) {
                    processAttachments(emailEntity, (MimeMultipart) message.getContent(), new AttachmentsCallBack() {
                        @Override
                        public void onSuccess(MimeMultipart content) {
                            try {
                                message.setContent(content);
                                request.setMimeMessage(message);
                                sesSendService.enqueue(request);
                            } catch (Throwable e) {
                                request.getSendHandler().onFailure(e);
                                eventBus.publish(SESEmailSentEvent.ADDRESS, new SESEmailSentEvent(request.getEmailEntity(), false));
                            }
                        }

                        @Override
                        public void onFailure(Throwable e) {
                            request.getSendHandler().onFailure(e);
                            eventBus.publish(SESEmailSentEvent.ADDRESS, new SESEmailSentEvent(request.getEmailEntity(), false));
                        }
                    });
                } else {
                    request.setMimeMessage(message);
                    sesSendService.enqueue(request);
                }
            } catch (Exception e) {
                request.getSendHandler().onFailure(e);
                eventBus.publish(SESEmailSentEvent.ADDRESS, new SESEmailSentEvent(request.getEmailEntity(), false));
            }
        }
    }

    private MimeMessage buildMimeMessage(EmailEntity emailEntity) throws Exception {
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
        return message;
    }

    private void processAttachments(EmailEntity emailEntity, MimeMultipart content, AttachmentsCallBack callBack) throws Exception {
        getAttachmentEntitiesObservable(emailEntity)
                .doOnError(callBack::onFailure)
                .doOnNext(emailAttachmentEntities -> downloadAttachments(content, emailAttachmentEntities, new AttachmentsCallBack() {
                    @Override
                    public void onSuccess(MimeMultipart content) {
                        callBack.onSuccess(content);
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        callBack.onFailure(e);
                    }
                }));
    }

    private Observable<List<EmailAttachmentEntity>> getAttachmentEntitiesObservable(EmailEntity emailEntity) {
        ObservableFuture<List<EmailAttachmentEntity>> observableFuture = RxHelper.observableFuture();
        getEmailAttachmentEntities(emailEntity, observableFuture.toHandler());
        return observableFuture;
    }

    private void getEmailAttachmentEntities(EmailEntity emailEntity, Handler<AsyncResult<List<EmailAttachmentEntity>>> completionHandler) {
        db.readObservable(session ->
                session.select(EMAIL_ATTACHMENT.fields()).from(EMAIL_ATTACHMENT)
                        .where(EMAIL_ATTACHMENT.EMAIL_ID.eq(emailEntity.getId()))
                        .fetch()
                        .into(EMAIL_ATTACHMENT)
                        .into(EmailAttachmentEntity.class))
                .doOnError(e -> {
                    if (completionHandler != null) {
                        completionHandler.handle(Future.failedFuture(e));
                    }
                })
                .subscribe(emailRecipientEntities -> {
                    if (completionHandler != null) {
                        completionHandler.handle(Future.succeededFuture(emailRecipientEntities));
                    }
                });
    }

    private void downloadAttachments(final MimeMultipart content, List<EmailAttachmentEntity> attachmentEntities, final AttachmentsCallBack callBack) {
        final AtomicInteger activeDownloads = new AtomicInteger();
        final AtomicBoolean failed = new AtomicBoolean();
        for (final EmailAttachmentEntity attachmentEntity : attachmentEntities) {
            if(failed.get())
                break;
            activeDownloads.incrementAndGet();
            SESAttachmentService.enqueue(new DownloadRequest(attachmentEntity.getFileId(), new DownloadCallBack() {
                @Override
                public void onSuccess(byte[] data) {
                    try {
                        Preconditions.checkNotNull(data);
                        final MimeBodyPart attachmentPart = new MimeBodyPart();
                        attachmentPart.setDescription(attachmentEntity.getDescription(), "UTF-8");
                        final DataSource ds = new ByteArrayDataSource(data, attachmentEntity.getMimeType());
                        attachmentPart.setDataHandler(new DataHandler(ds));
                        attachmentPart.setHeader("Content-ID", "<" + attachmentEntity.getId() + ">");
                        attachmentPart.setFileName(attachmentEntity.getName());
                        content.addBodyPart(attachmentPart);

                        if(activeDownloads.decrementAndGet() <= 0){
                            callBack.onSuccess(content);
                        }
                    } catch (Exception e) {
                        failed.set(true);
                        callBack.onFailure(e);
                    }
                }

                @Override
                public void onFailure(Throwable e) {
                    failed.set(true);
                    callBack.onFailure(e);
                }
            }));
        }
    }

    interface AttachmentsCallBack {
        void onSuccess(MimeMultipart content);
        void onFailure(Throwable e);
    }
}
