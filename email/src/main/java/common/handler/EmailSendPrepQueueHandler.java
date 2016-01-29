package common.handler;

import common.data.SendRequest;
import data.schema.Tables;
import entity.EmailAttachmentEntity;
import entity.EmailEntity;
import io.clickhandler.queue.QueueHandler;
import io.clickhandler.sql.db.SqlExecutor;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import rx.Observable;

import java.util.List;

/**
 * @author Brad Behnke
 */
public abstract class EmailSendPrepQueueHandler<T extends SendRequest> implements QueueHandler<T>, Tables {

    private SqlExecutor db;

    public EmailSendPrepQueueHandler(SqlExecutor db) {
        this.db = db;
    }

    protected Observable<List<EmailAttachmentEntity>> getAttachmentEntitiesObservable(EmailEntity emailEntity) {
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
}
