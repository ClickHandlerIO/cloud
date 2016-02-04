package io.clickhandler.email.sns.handler;

import io.clickhandler.email.common.data.Message;
import io.clickhandler.email.entity.EmailEntity;
import io.clickhandler.email.entity.EmailRecipientEntity;
import io.clickhandler.email.entity.RecipientStatus;
import io.clickhandler.email.schema.Tables;
import io.clickhandler.email.sns.config.SNSConfig;
import io.clickhandler.email.sns.data.json.common.MessageType;
import io.clickhandler.email.sns.data.json.email.notify.*;
import io.clickhandler.email.sns.data.json.email.receive.EmailReceivedMessage;
import io.clickhandler.email.sns.data.json.general.GeneralMessage;
import io.clickhandler.email.sns.event.email.SNSEmailBounceEvent;
import io.clickhandler.email.sns.event.email.SNSEmailComplaintEvent;
import io.clickhandler.email.sns.event.email.SNSEmailDeliveryEvent;
import io.clickhandler.email.sns.event.email.SNSEmailReceivedEvent;
import io.clickhandler.email.sns.event.general.SNSNotificationEvent;
import io.clickhandler.email.sns.event.general.SNSSubscriptionConfirmEvent;
import io.clickhandler.email.sns.event.general.SNSUnsubscribeConfirmEvent;
import io.clickhandler.queue.QueueHandler;
import io.clickhandler.sql.SqlExecutor;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.core.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

/**
 * Filters SNS Messages to configured list's topic ARNs, and fires Vertx EventBus events for accepted messages.
 * <p/>
 * Auto confirms subscriptions found in generalSubscription list.
 * Auto confirms unsubscribes for topics not found in generalSubscription list.
 * <p/>
 * Updates email records on: Bounce, Complaint, Delivery.
 *
 * @author Brad Behnke
 */
public class SNSQueueHandler implements QueueHandler<Message>, Tables {

    public static final Logger LOG = LoggerFactory.getLogger(SNSQueueHandler.class);
    private final List<String> generalSubscriptionArnList;
    private final List<String> emailNotifySubscriptionArnList;
    private final List<String> emailReceivedSubscriptionArnList;

    private final SqlExecutor db;
    private final EventBus eventBus;

    public SNSQueueHandler(SNSConfig snsConfig, EventBus eventBus, SqlExecutor db) {
        this.db = db;
        this.eventBus = eventBus;
        this.generalSubscriptionArnList = snsConfig.getGeneralSubscriptionArnList();
        this.emailNotifySubscriptionArnList = snsConfig.getEmailNotifySubscriptionArnList();
        this.emailReceivedSubscriptionArnList = snsConfig.getEmailReceivedSubscriptionArnList();
    }

    @Override
    public void receive(List<Message> messages) {
        messages.forEach(this::handle);
    }

    public void handle(Message message) {
        if (message == null) {
            return;
        }
        if (message instanceof GeneralMessage) {
            handleMessage((GeneralMessage) message);
        }
        if (message instanceof EmailNotifyMessage) {
            handleMessage((EmailNotifyMessage) message);
        }
        if (message instanceof EmailReceivedMessage) {
            handleMessage((EmailReceivedMessage) message);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Notification, Subscribe, and Unsubscribe Handling
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void handleMessage(GeneralMessage message) {
        if (message == null || message.getType() == null) {
            LOG.error("Invalid GeneralMessage Received");
            return;
        }
        switch (MessageType.getTypeEnum(message.getType())) {
            case SUB_CONFIRM:
                if (generalSubscriptionArnList.contains(message.getTopicArn())) {
                    handleSubscribe(message);
                }
                break;
            case UNSUB_CONFIRM:
                if (!generalSubscriptionArnList.contains(message.getTopicArn())) {
                    handleUnsubscribe(message);
                }
                break;
            case NOTIFICATION:
                if (generalSubscriptionArnList.contains(message.getTopicArn())) {
                    handleNotification(message);
                }
                break;
            default:
                LOG.error("Invalid GeneralMessage Received with Type: " + message.getType());
                break;
        }
    }

    private void handleSubscribe(GeneralMessage message) {
        try {
            // open subscription URL to confirm.
            Scanner sc = new Scanner(new URL(message.getSubscribeURL()).openStream());
            // Capture XML body returned.
            StringBuilder sb = new StringBuilder();
            while (sc.hasNextLine()) {
                sb.append(sc.nextLine());
            }
            // TODO ensure XML body reflects successful confirmation.
        } catch (Exception e) {
            LOG.error("Failed to Confirm Subscription to Topic ARN: " + message.getTopicArn());
        }
        eventBus.publish(SNSSubscriptionConfirmEvent.ADDRESS, new SNSSubscriptionConfirmEvent(message).toJson());
    }

    private void handleUnsubscribe(GeneralMessage message) {
        try {
            // open subscription URL
            Scanner sc = new Scanner(new URL(message.getUnsubscribeURL()).openStream());
            // Capture XML body returned.
            StringBuilder sb = new StringBuilder();
            while (sc.hasNextLine()) {
                sb.append(sc.nextLine());
            }
            // TODO ensure XML body reflects successful unsubcribe or subscribe.
        } catch (Exception e) {
            LOG.error("Failed to Confirm Unsubscribe to Topic ARN: " + message.getTopicArn());
        }
        eventBus.publish(SNSUnsubscribeConfirmEvent.ADDRESS, new SNSUnsubscribeConfirmEvent(message).toJson());
    }

    private void handleNotification(GeneralMessage message) {
        eventBus.publish(SNSNotificationEvent.ADDRESS, new SNSNotificationEvent(message).toJson());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Email Status Notification Handling
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void handleMessage(EmailNotifyMessage message) {
        if (message == null || message.getNotificationType() == null) {
            LOG.error("Invalid EmailNotifyMessage Received");
            return;
        }
        if (!emailNotifySubscriptionArnList.contains(message.getMail().getSourceArn())) {
            return;
        }
        switch (MessageType.getTypeEnum(message.getNotificationType())) {
            case DELIVERY:
                handleDelivery(message);
                break;
            case BOUNCE:
                handleBounce(message);
                break;
            case COMPLAINT:
                handleComplaint(message);
                break;
            default:
                LOG.error("Invalid EmailNotifyMessage Received with Type: " + message.getNotificationType());
                break;
        }
    }

    private void handleDelivery(EmailNotifyMessage message) {
        final NotifyMail mail = message.getMail();
        final Delivery delivery = message.getDelivery();
        getRecipientsObservable(mail)
            .doOnError(e -> LOG.error(e.getMessage()))
            .doOnNext(emailRecipientEntities -> emailRecipientEntities.stream().filter(recipientEntity -> delivery.getRecipients().contains(recipientEntity.getAddress())).forEach(recipientEntity -> {
                recipientEntity.setStatus(RecipientStatus.DELIVERED);
                recipientEntity.setDelivered(new Date());
                updateRecipientObservable(recipientEntity)
                    .doOnError(throwable -> LOG.error(throwable.getMessage()));
            }));
        eventBus.publish(SNSEmailDeliveryEvent.ADDRESS, new SNSEmailDeliveryEvent(message).toJson());
    }

    private void handleBounce(EmailNotifyMessage message) {
        final NotifyMail mail = message.getMail();
        final Bounce bounce = message.getBounce();
        final List<String> bouncedRecipients = bounce.getRecipientsStrings();

        getRecipientsObservable(mail)
            .doOnError(e -> LOG.error(e.getMessage()))
            .doOnNext(emailRecipientEntities -> emailRecipientEntities.stream().filter(recipientEntity -> bouncedRecipients.contains(recipientEntity.getAddress())).forEach(recipientEntity -> {
                recipientEntity.setStatus(RecipientStatus.BOUNCED);
                recipientEntity.setBounced(new Date());
                updateRecipientObservable(recipientEntity)
                    .doOnError(throwable -> LOG.error(throwable.getMessage()));
            }));
        eventBus.publish(SNSEmailBounceEvent.ADDRESS, new SNSEmailBounceEvent(message).toJson());
    }

    private void handleComplaint(EmailNotifyMessage message) {
        final NotifyMail mail = message.getMail();
        final Complaint complaint = message.getComplaint();
        final List<String> complainedRecipients = complaint.getRecipientsStrings();
        getRecipientsObservable(mail)
            .doOnError(e -> LOG.error(e.getMessage()))
            .doOnNext(emailRecipientEntities -> emailRecipientEntities.stream().filter(recipientEntity -> complainedRecipients.contains(recipientEntity.getAddress())).forEach(recipientEntity -> {
                recipientEntity.setStatus(RecipientStatus.COMPLAINT);
                recipientEntity.setComplaint(new Date());
                updateRecipientObservable(recipientEntity)
                    .doOnError(throwable -> LOG.error(throwable.getMessage()));
            }));
        eventBus.publish(SNSEmailComplaintEvent.ADDRESS, new SNSEmailComplaintEvent(message).toJson());
    }

    private Observable<List<EmailRecipientEntity>> getRecipientsObservable(NotifyMail mail) {
        ObservableFuture<List<EmailRecipientEntity>> observableFuture = RxHelper.observableFuture();
        getRecipients(mail, observableFuture.toHandler());
        return observableFuture;
    }

    private void getRecipients(NotifyMail mail, Handler<AsyncResult<List<EmailRecipientEntity>>> completionHandler) {
        db.read(session -> session.select(EMAIL.fields()).from(EMAIL).where(EMAIL.MESSAGE_ID.eq(mail.getMessageId())).fetchAny().into(EMAIL).into(EmailEntity.class))
            .doOnError(throwable -> {
                if (completionHandler != null) {
                    completionHandler.handle(Future.failedFuture(throwable));
                }
            })
            .doOnNext(emailEntity -> db.readObservable(session -> session.select(EMAIL_RECIPIENT.fields()).from(EMAIL_RECIPIENT)
                .where(EMAIL_RECIPIENT.EMAIL_ID.eq(emailEntity.getId()))
                .fetch().into(EMAIL_RECIPIENT).into(EmailRecipientEntity.class))
                .doOnError(throwable -> {
                    if (completionHandler != null) {
                        completionHandler.handle(Future.failedFuture(throwable));
                    }
                })
                .doOnNext(emailRecipientEntities -> {
                    if (completionHandler != null) {
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
        db.write(session -> session.update(recipientEntity))
            .doOnError(throwable -> {
                if (completionHandler != null) {
                    completionHandler.handle(Future.failedFuture(throwable));
                }
            })
            .doOnNext(recipientEntitySqlResult -> {
                if (completionHandler != null) {
                    if (recipientEntitySqlResult.isSuccess()) {
                        completionHandler.handle(Future.succeededFuture(recipientEntity));
                    } else {
                        completionHandler.handle(Future.failedFuture(new Exception("EmailRecipientEntity Update Failed.")));
                    }
                }
            });
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Email Received Notification Handling
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void handleMessage(EmailReceivedMessage message) {
        if (message == null || message.getNotificationType() == null) {
            LOG.error("Invalid EmailReceivedMessage Received");
            return;
        }
        if (!emailReceivedSubscriptionArnList.contains(message.getReceipt().getAction().getTopicArn())) {
            return;
        }
        switch (MessageType.getTypeEnum(message.getNotificationType())) {
            case RECEIVED:
                handleReceived(message);
                break;
            default:
                LOG.error("Invalid EmailReceivedMessage Received with Type: " + message.getNotificationType());
                break;
        }
    }

    private void handleReceived(EmailReceivedMessage message) {
        eventBus.publish(SNSEmailReceivedEvent.ADDRESS, new SNSEmailReceivedEvent(message).toJson());
    }

}
