package io.clickhandler.files.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.clickhandler.email.common.data.SendRequest;
import io.clickhandler.email.entity.*;
import io.clickhandler.email.schema.Tables;
import io.clickhandler.files.service.FileService;
import io.clickhandler.queue.QueueHandler;
import io.clickhandler.sql.SqlExecutor;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.Date;
import java.util.List;

/**
 * @author Brad Behnke
 */
public abstract class EmailSendQueueHandler<T extends SendRequest> implements QueueHandler<T>, Tables {

    protected final static ObjectMapper jsonMapper = new ObjectMapper();
    private final static Logger LOG = LoggerFactory.getLogger(EmailSendQueueHandler.class);
    private final SqlExecutor db;
    private final FileService fileService;

    public EmailSendQueueHandler(SqlExecutor db, FileService fileService) {
        this.db = db;
        this.fileService = fileService;
    }

    protected void failure(SendRequest sendRequest, Throwable throwable) {
        if (sendRequest.getCompletionHandler() != null) {
            sendRequest.getCompletionHandler().handle(Future.failedFuture(throwable));
        }
    }

    protected void success(SendRequest sendRequest, EmailEntity emailEntity) {
        if (sendRequest.getCompletionHandler() != null) {
            sendRequest.getCompletionHandler().handle(Future.succeededFuture(emailEntity));
        }
    }

    protected Observable<List<EmailAttachmentEntity>> getAttachmentEntitiesObservable(EmailEntity emailEntity) {
        ObservableFuture<List<EmailAttachmentEntity>> observableFuture = RxHelper.observableFuture();
        getEmailAttachmentEntities(emailEntity, observableFuture.toHandler());
        return observableFuture;
    }

    private void getEmailAttachmentEntities(EmailEntity emailEntity, Handler<AsyncResult<List<EmailAttachmentEntity>>> completionHandler) {
        db.read(session ->
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

    protected Observable<FileEntity> getFileEntityObservable(String fileId) {
        ObservableFuture<FileEntity> observableFuture = RxHelper.observableFuture();
        getFileEntity(fileId, observableFuture.toHandler());
        return observableFuture;
    }

    private void getFileEntity(String fileId, Handler<AsyncResult<FileEntity>> completionHandler) {
        db.readObservable(session ->
            session.getEntity(FileEntity.class, fileId))
            .doOnError(e -> {
                if (completionHandler != null) {
                    completionHandler.handle(Future.failedFuture(e));
                }
            })
            .subscribe(fileEntity -> {
                if (completionHandler != null) {
                    completionHandler.handle(Future.succeededFuture(fileEntity));
                }
            });
    }

    protected void updateRecords(EmailEntity emailEntity) {
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
        db.write(session -> session.update(emailEntity))
            .doOnError(throwable -> {
                if (completionHandler != null) {
                    completionHandler.handle(Future.failedFuture(throwable));
                }
            })
            .doOnNext(emailEntitySqlResult -> {
                if (completionHandler != null) {
                    if (emailEntitySqlResult.isSuccess()) {
                        completionHandler.handle(Future.succeededFuture(emailEntity));
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
        db.read(session ->
            session.select(EMAIL_RECIPIENT.fields()).from(EMAIL_RECIPIENT)
                .where(EMAIL_RECIPIENT.EMAIL_ID.eq(emailId))
                .fetch().into(EMAIL_RECIPIENT).into(EmailRecipientEntity.class))
            .doOnError(throwable -> {
                if (completionHandler != null) {
                    completionHandler.handle(Future.failedFuture(throwable));
                }
            })
            .doOnNext(emailRecipientEntities -> {
                for (EmailRecipientEntity recipientEntity : emailRecipientEntities) {
                    if (success) {
                        recipientEntity.setStatus(RecipientStatus.SENT);
                        recipientEntity.setSent(new Date());
                    } else {
                        recipientEntity.setStatus(RecipientStatus.FAILED);
                        recipientEntity.setFailed(new Date());
                    }
                    db.write(session -> session.update(recipientEntity))
                        .doOnError(throwable -> {
                            if (completionHandler != null) {
                                completionHandler.handle(Future.failedFuture(throwable));
                            }
                        })
                        .doOnNext(integerSqlResult -> {
                            if (completionHandler != null) {
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

    protected FileService getFileService() {
        return fileService;
    }

}
