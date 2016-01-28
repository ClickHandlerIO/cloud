package ses.handler;

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
    private final SESAttachmentService sesAttachmentService;
    private final SESSendService sesSendService;

    public SESPrepQueueHandler(EventBus eventBus, SqlDatabase db, SESAttachmentService sesAttachmentService, SESSendService sesSendService) {
        this.eventBus = eventBus;
        this.db = db;
        this.sesAttachmentService = sesAttachmentService;
        this.sesSendService = sesSendService;
    }

    @Override
    public void receive(List<SESSendRequest> sendRequests) {
        sendRequests.forEach(this::processRequest);
    }

    private void processRequest(SESSendRequest request) {
        final EmailEntity emailEntity = request.getEmailEntity();
        buildMimeMessageObservable(emailEntity)
                .doOnError(throwable -> {
                    if(request.getCompletionHandler() != null) {
                        request.getCompletionHandler().handle(Future.failedFuture(throwable));
                    }
                    eventBus.publish(SESEmailSentEvent.ADDRESS, new SESEmailSentEvent(request.getEmailEntity(), false));
                })
                .doOnNext(message -> {
                    try {
                        if (emailEntity.isAttachments()) {
                            processAttachmentsObservable(emailEntity, (MimeMultipart) message.getContent())
                                    .doOnError(throwable -> {
                                        if(request.getCompletionHandler() != null) {
                                            request.getCompletionHandler().handle(Future.failedFuture(throwable));
                                        }
                                        eventBus.publish(SESEmailSentEvent.ADDRESS, new SESEmailSentEvent(request.getEmailEntity(), false));
                                    })
                                    .doOnNext(mimeMultipart -> {
                                        try {
                                            message.setContent(mimeMultipart);
                                            request.setMimeMessage(message);
                                            sesSendService.enqueue(request);
                                        } catch (Throwable throwable) {
                                            if(request.getCompletionHandler() != null) {
                                                request.getCompletionHandler().handle(Future.failedFuture(throwable));
                                            }
                                            eventBus.publish(SESEmailSentEvent.ADDRESS, new SESEmailSentEvent(request.getEmailEntity(), false));
                                        }
                                    });
                        } else {
                            request.setMimeMessage(message);
                            sesSendService.enqueue(request);
                        }
                    } catch (Throwable throwable) {
                        if(request.getCompletionHandler() != null) {
                            request.getCompletionHandler().handle(Future.failedFuture(throwable));
                        }
                        eventBus.publish(SESEmailSentEvent.ADDRESS, new SESEmailSentEvent(request.getEmailEntity(), false));
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

    private Observable<MimeMultipart> processAttachmentsObservable(EmailEntity emailEntity, MimeMultipart content) {
        ObservableFuture<MimeMultipart> observableFuture = RxHelper.observableFuture();
        processAttachments(emailEntity, content, observableFuture.toHandler());
        return observableFuture;
    }

    private void processAttachments(EmailEntity emailEntity, MimeMultipart content, Handler<AsyncResult<MimeMultipart>> completionHandler) {
        getAttachmentEntitiesObservable(emailEntity)
                .doOnError(throwable -> {
                    if(completionHandler != null) {
                        completionHandler.handle(Future.failedFuture(throwable));
                    }
                })
                .doOnNext(attachmentEntities -> downloadAndBuildAttachmentsObservable(content, attachmentEntities)
                        .doOnError(throwable -> {
                            if(completionHandler != null) {
                                completionHandler.handle(Future.failedFuture(throwable));
                            }
                        })
                        .doOnNext(mimeMultipart -> {
                            if(completionHandler != null) {
                                completionHandler.handle(Future.succeededFuture(mimeMultipart));
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


    private Observable<MimeMultipart> downloadAndBuildAttachmentsObservable(MimeMultipart content, List<EmailAttachmentEntity> attachmentEntities) {
        ObservableFuture<MimeMultipart> observableFuture = RxHelper.observableFuture();
        downloadAndBuildAttachments(content, attachmentEntities, observableFuture.toHandler());
        return observableFuture;
    }

    private void downloadAndBuildAttachments(MimeMultipart content, List<EmailAttachmentEntity> attachmentEntities, Handler<AsyncResult<MimeMultipart>> completionHandler) {
        final AtomicInteger activeDownloads = new AtomicInteger();
        final AtomicBoolean failed = new AtomicBoolean();
        for(EmailAttachmentEntity attachmentEntity:attachmentEntities) {
            if(failed.get())
                break;
            activeDownloads.incrementAndGet();
            sesAttachmentService.downloadObservable(attachmentEntity.getFileId())
                    .doOnError(throwable -> {
                        if(completionHandler != null) {
                            completionHandler.handle(Future.failedFuture(throwable));
                        }
                    })
                    .doOnNext(buffer -> {
                        try {
                            final MimeBodyPart attachmentPart = new MimeBodyPart();
                            attachmentPart.setDescription(attachmentEntity.getDescription(), "UTF-8");
                            final DataSource ds = new ByteArrayDataSource(buffer.getBytes(), attachmentEntity.getMimeType());
                            attachmentPart.setDataHandler(new DataHandler(ds));
                            attachmentPart.setHeader("Content-ID", "<" + attachmentEntity.getId() + ">");
                            attachmentPart.setFileName(attachmentEntity.getName());
                            content.addBodyPart(attachmentPart);

                            if(activeDownloads.decrementAndGet() <= 0){
                                if(completionHandler != null) {
                                    completionHandler.handle(Future.succeededFuture(content));
                                }
                            }
                        } catch (Throwable e) {
                            failed.set(true);
                            if(completionHandler != null) {
                                completionHandler.handle(Future.failedFuture(e));
                            }
                        }
                    });
        }
    }
}
