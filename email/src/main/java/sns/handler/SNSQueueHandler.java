package sns.handler;

import data.schema.Tables;
import entity.*;
import io.clickhandler.queue.QueueHandler;
import io.clickhandler.sql.db.*;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.core.eventbus.EventBus;
import rx.Observable;
import sns.event.email.SNSEmailBounceEvent;
import sns.event.email.SNSEmailComplaintEvent;
import sns.event.email.SNSEmailDeliveryEvent;
import sns.event.email.SNSEmailReceivedEvent;
import sns.event.general.SNSNotificationEvent;
import sns.event.general.SNSSubscriptionConfirmEvent;
import sns.event.general.SNSUnsubscribeConfirmEvent;
import org.jooq.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sns.config.SNSConfig;
import sns.data.json.common.Message;
import sns.data.json.common.MessageType;
import sns.data.json.email.notify.*;
import sns.data.json.email.receive.EmailReceivedMessage;
import sns.data.json.general.GeneralMessage;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

/**
 * Filters SNS Messages to configured list's topic ARNs, and fires Vertx EventBus events for accepted messages.
 * <p>
 * Auto confirms subscriptions found in generalSubscription list.
 * Auto confirms unsubscribes for topics not found in generalSubscription list.
 * <p>
 * Updates email records on: Bounce, Complaint, Delivery.
 *
 * @author Brad Behnke
 */
public class SNSQueueHandler implements QueueHandler<Message>, Tables {

    public static final Logger LOG = LoggerFactory.getLogger(SNSQueueHandler.class);
    private final List<String> generalSubscriptionArnList;
    private final List<String> emailNotifySubscriptionArnList;
    private final List<String> emailReceivedSubscriptionArnList;

    private final SqlDatabase db;
    private final EventBus eventBus;

    public SNSQueueHandler(EventBus eventBus, SqlDatabase db) {
        this.db = db;
        this.eventBus = eventBus;
        this.generalSubscriptionArnList = SNSConfig.getGeneralSubscriptionArnList();
        this.emailNotifySubscriptionArnList = SNSConfig.getEmailNotifySubscriptionArnList();
        this.emailReceivedSubscriptionArnList = SNSConfig.getEmailReceivedSubscriptionArnList();
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
            LOG.error("Failed to confirm subscription to topic ARN: " + message.getTopicArn());
        }
        eventBus.publish(SNSSubscriptionConfirmEvent.ADDRESS, new SNSSubscriptionConfirmEvent(message));
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
            LOG.error("Failed to handle unsubscribe to topic ARN: " + message.getTopicArn());
        }
        eventBus.publish(SNSUnsubscribeConfirmEvent.ADDRESS, new SNSUnsubscribeConfirmEvent(message));
    }

    private void handleNotification(GeneralMessage message) {
        eventBus.publish(SNSNotificationEvent.ADDRESS, new SNSNotificationEvent(message));
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
                .doOnError(e -> LOG.error("Failed to retrieve email recipients", e))
                .doOnNext(emailRecipientEntities -> emailRecipientEntities.stream().filter(recipientEntity -> delivery.getRecipients().contains(recipientEntity.getAddress())).forEach(recipientEntity -> {
                    recipientEntity.setStatus(RecipientStatus.DELIVERED);
                    recipientEntity.setDelivered(new Date());
                    updateRecipient(recipientEntity);
                }));
        eventBus.publish(SNSEmailDeliveryEvent.ADDRESS, new SNSEmailDeliveryEvent(message));
    }

    private void handleBounce(EmailNotifyMessage message) {
        final NotifyMail mail = message.getMail();
        final Bounce bounce = message.getBounce();
        final List<String> bouncedRecipients = bounce.getRecipientsStrings();

        getRecipientsObservable(mail)
                .doOnError(e -> LOG.error("Failed to retrieve email recipients", e))
                .doOnNext(emailRecipientEntities -> emailRecipientEntities.stream().filter(recipientEntity -> bouncedRecipients.contains(recipientEntity.getAddress())).forEach(recipientEntity -> {
                    recipientEntity.setStatus(RecipientStatus.BOUNCED);
                    recipientEntity.setBounced(new Date());
                    updateRecipient(recipientEntity);
                }));

        eventBus.publish(SNSEmailBounceEvent.ADDRESS, new SNSEmailBounceEvent(message));
    }

    private void handleComplaint(EmailNotifyMessage message) {
        final NotifyMail mail = message.getMail();
        final Complaint complaint = message.getComplaint();
        final List<String> complainedRecipients = complaint.getRecipientsStrings();
        getRecipientsObservable(mail)
                .doOnError(e -> LOG.error("Failed to retrieve email recipients", e))
                .doOnNext(emailRecipientEntities -> emailRecipientEntities.stream().filter(recipientEntity -> complainedRecipients.contains(recipientEntity.getAddress())).forEach(recipientEntity -> {
                    recipientEntity.setStatus(RecipientStatus.BOUNCED);
                    recipientEntity.setBounced(new Date());
                    updateRecipient(recipientEntity);
                }));
        eventBus.publish(SNSEmailComplaintEvent.ADDRESS, new SNSEmailComplaintEvent(message));
    }

    private Observable<List<EmailRecipientEntity>> getRecipientsObservable(NotifyMail mail) {
        ObservableFuture<List<EmailRecipientEntity>> observableFuture = RxHelper.observableFuture();
        getRecipients(mail, observableFuture.toHandler());
        return observableFuture;
    }

    private void getRecipients(NotifyMail mail, Handler<AsyncResult<List<EmailRecipientEntity>>> completionHandler) {
        db.readObservable(session -> {
            Record record = session.select(EMAIL.fields()).from(EMAIL).where(EMAIL.MESSAGE_ID.eq(mail.getMessageId())).fetchAny();
            if (record == null) {
                LOG.error("Email Record Not Found for MessageId: " + mail.getMessageId());
                return null;
            }
            return record.into(EMAIL).into(EmailEntity.class);
        }).subscribe(emailEntity -> {
            db.readObservable(session ->
                    session.select(EMAIL_RECIPIENT.fields()).from(EMAIL_RECIPIENT)
                            .where(EMAIL_RECIPIENT.EMAIL_ID.eq(emailEntity.getId()))
                            .fetch()
                            .into(EMAIL_RECIPIENT)
                            .into(EmailRecipientEntity.class))
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
        });
    }

    private void updateRecipient(final EmailRecipientEntity recipientEntity) {
        db.writeObservable(session -> new SqlResult<>(session.update(recipientEntity) == 1, recipientEntity)).subscribe(result -> {
            if (!result.isSuccess()) {
                LOG.error("Failed to update email recipient.");
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
        eventBus.publish(SNSEmailReceivedEvent.ADDRESS, new SNSEmailReceivedEvent(message));
    }

}
