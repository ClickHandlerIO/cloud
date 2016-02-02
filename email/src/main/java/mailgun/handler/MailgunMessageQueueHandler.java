package mailgun.handler;

import common.data.Message;
import data.schema.Tables;
import entity.EmailEntity;
import entity.EmailRecipientEntity;
import entity.RecipientStatus;
import io.clickhandler.queue.QueueHandler;
import io.clickhandler.sql.SqlExecutor;
import io.clickhandler.sql.SqlResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import mailgun.data.BounceMessage;
import mailgun.data.DeliveryMessage;
import mailgun.data.FailureMessage;
import mailgun.data.ReceiveMessage;
import mailgun.event.MailgunEmailBounceEvent;
import mailgun.event.MailgunEmailDeliveryEvent;
import mailgun.event.MailgunEmailFailureEvent;
import mailgun.event.MailgunEmailReceiveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.Date;
import java.util.List;

/**
 * Handles capture mailgun notification messages.
 * Updates email records on: Delivery.
 * Fires events for each message type.
 *
 * @author Brad Behnke
 */
public class MailgunMessageQueueHandler implements QueueHandler<Message>, Tables {

    public static final Logger LOG = LoggerFactory.getLogger(MailgunMessageQueueHandler.class);

    private final SqlExecutor db;
    private final EventBus eventBus;

    public MailgunMessageQueueHandler(EventBus eventBus, SqlExecutor db) {
        this.db = db;
        this.eventBus = eventBus;
    }

    @Override
    public void receive(List<Message> messages) {
        messages.forEach(this::handle);
    }

    public void handle(Message message) {
        if (message == null) {
            return;
        }
        if (message instanceof DeliveryMessage) {
            handleDelivery((DeliveryMessage) message);
        }
        if (message instanceof BounceMessage) {
            handleBounce((BounceMessage) message);
        }
        if (message instanceof FailureMessage) {
            handleFailure((FailureMessage) message);
        }
        if (message instanceof ReceiveMessage) {
            handleReceive((ReceiveMessage) message);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Email Status Notification Handling
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void handleDelivery(DeliveryMessage message) {
        getRecipientsObservable(message.getMessageId())
                .doOnError(e -> LOG.error(e.getMessage()))
                .doOnNext(emailRecipientEntities -> emailRecipientEntities.stream().filter(recipientEntity -> message.getRecipient().equalsIgnoreCase(recipientEntity.getAddress())).forEach(recipientEntity -> {
                    recipientEntity.setStatus(RecipientStatus.DELIVERED);
                    recipientEntity.setDelivered(new Date());
                    updateRecipientObservable(recipientEntity)
                            .doOnError(throwable -> LOG.error(throwable.getMessage()));
                }));
        eventBus.publish(MailgunEmailDeliveryEvent.ADDRESS, new MailgunEmailDeliveryEvent(message).toJson());
    }

    private void handleBounce(BounceMessage message) {
        // todo what to update ? no message id comes over in notification
        eventBus.publish(MailgunEmailBounceEvent.ADDRESS, new MailgunEmailBounceEvent(message).toJson());
    }

    private void handleFailure(FailureMessage message) {
        // todo what to update ? no message id comes over in notification
        eventBus.publish(MailgunEmailFailureEvent.ADDRESS, new MailgunEmailFailureEvent(message).toJson());
    }

    private void handleReceive(ReceiveMessage message) {
        // todo create record of email received?
        eventBus.publish(MailgunEmailReceiveEvent.ADDRESS, new MailgunEmailReceiveEvent(message).toJson());
    }

    private Observable<List<EmailRecipientEntity>> getRecipientsObservable(String messageId) {
        ObservableFuture<List<EmailRecipientEntity>> observableFuture = RxHelper.observableFuture();
        getRecipients(messageId, observableFuture.toHandler());
        return observableFuture;
    }

    private void getRecipients(String messageId, Handler<AsyncResult<List<EmailRecipientEntity>>> completionHandler) {
        db.readObservable(session -> session.select(EMAIL.fields()).from(EMAIL).where(EMAIL.MESSAGE_ID.eq(messageId)).fetchAny().into(EMAIL).into(EmailEntity.class))
                .doOnError(throwable -> {
                    if(completionHandler != null) {
                        completionHandler.handle(Future.failedFuture(throwable));
                    }
                })
                .doOnNext(emailEntity -> db.readObservable(session -> session.select(EMAIL_RECIPIENT.fields()).from(EMAIL_RECIPIENT)
                        .where(EMAIL_RECIPIENT.EMAIL_ID.eq(emailEntity.getId()))
                        .fetch().into(EMAIL_RECIPIENT).into(EmailRecipientEntity.class))
                        .doOnError(throwable -> {
                            if(completionHandler != null) {
                                completionHandler.handle(Future.failedFuture(throwable));
                            }
                        })
                        .doOnNext(emailRecipientEntities -> {
                            if(completionHandler != null){
                                completionHandler.handle(Future.succeededFuture(emailRecipientEntities));
                            }
                        }));
    }

    private Observable<EmailRecipientEntity> updateRecipientObservable(EmailRecipientEntity recipientEntity) {
        ObservableFuture<EmailRecipientEntity> observableFuture = RxHelper.observableFuture();
        updateRecipient(recipientEntity, observableFuture.toHandler());
        return observableFuture;
    }

    private void updateRecipient(final EmailRecipientEntity recipientEntity, Handler<AsyncResult<EmailRecipientEntity>> completionHandler) {
        db.writeObservable(session -> {
                        Integer result = session.update(recipientEntity);
                        return new SqlResult<>(result == 1, recipientEntity);
                })
                .doOnError(throwable -> {
                    if(completionHandler != null) {
                        completionHandler.handle(Future.failedFuture(throwable));
                    }
                })
                .doOnNext(recipientEntitySqlResult -> {
                    if(completionHandler != null) {
                        if (recipientEntitySqlResult.isSuccess()) {
                            completionHandler.handle(Future.succeededFuture(recipientEntitySqlResult.get()));
                        } else {
                            completionHandler.handle(Future.failedFuture(new Exception("EmailRecipientEntity Update Failed.")));
                        }
                    }
                });
    }

}
