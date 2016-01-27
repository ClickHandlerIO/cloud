package sns.handler;

import data.schema.Tables;
import entity.*;
import io.clickhandler.queue.QueueHandler;
import io.clickhandler.sql.db.Database;
import io.clickhandler.sql.db.DatabaseSession;
import io.vertx.rxjava.core.eventbus.EventBus;
import sns.json.*;
import org.jooq.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sns.config.SNSConfig;
import sns.event.*;
import sns.json.email.notify.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author Brad Behnke
 */
@Singleton
public class SNSQueueHandler implements QueueHandler<Message>, Tables {

    public static final Logger LOG = LoggerFactory.getLogger(SNSQueueHandler.class);
    private final List<String> generalSubscriptionArnList;
    private final List<String> emailSubscriptionArnList;

    private final DatabaseSession db;
    private final EventBus eventBus;

    @Inject
    public SNSQueueHandler(EventBus eventBus, Database db) {
        this.db = db.getSession();
        this.eventBus = eventBus;
        this.generalSubscriptionArnList = SNSConfig.getGeneralSubscriptionArnList();
        this.emailSubscriptionArnList = SNSConfig.getEmailSubscriptionArnList();
    }

    @Override
    public void receive(List<Message> messages) {
        messages.forEach(this::handle);
    }

    private void handle(Message message) {

        if(message instanceof GeneralMessage) {
            handleMessage((GeneralMessage) message);
        }
        if(message instanceof EmailNotifyMessage) {
            handleMessage((EmailNotifyMessage) message);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Notification, Subscribe, and Unsubscribe Handling
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void handleMessage(GeneralMessage message) {
        if(message == null || message.getType() == null) {
            LOG.error("Invalid GeneralMessage Received");
            return;
        }
        switch (MessageType.getTypeEnum(message.getType())) {
            case SUB_CONFIRM:
                if(generalSubscriptionArnList.contains(message.getTopicArn())) {
                    handleSubscribe(message);
                }
                break;
            case UNSUB_CONFIRM:
                if(!generalSubscriptionArnList.contains(message.getTopicArn())) {
                    handleUnsubscribe(message);
                }
                break;
            case NOTIFICATION:
                if(generalSubscriptionArnList.contains(message.getTopicArn())) {
                    handleNotification(message);
                }
                break;
            default:
                break;
        }
    }

    protected void handleSubscribe(GeneralMessage message) {
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
    }

    protected void handleUnsubscribe(GeneralMessage message) {
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
    }

    protected void handleNotification(GeneralMessage message) {
        eventBus.publish(NotificationEvent.ADDRESS, new NotificationEvent(message));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Email Status Notification Handling
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected void handleMessage(EmailNotifyMessage message) {
        if(message == null || message.getNotificationType() == null) {
            LOG.error("Invalid EmailNotifyMessage Received");
            return;
        }
        if(!emailSubscriptionArnList.contains(message.getMail().getSourceArn())) {
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

    protected void handleDelivery(EmailNotifyMessage message) {
        final NotifyMail mail = message.getMail();
        final Delivery delivery = message.getDelivery();
        List<EmailRecipientEntity> recipientEntities = getRecipients(mail);
        recipientEntities.stream().filter(recipientEntity -> delivery.getRecipients().contains(recipientEntity.getAddress())).forEach(recipientEntity -> {
            recipientEntity.setStatus(RecipientStatus.DELIVERED);
            recipientEntity.setDelivered(new Date());
            db.update(recipientEntity);
        });
        eventBus.publish(EmailDeliveryEvent.ADDRESS, new EmailDeliveryEvent(message));
    }

    protected void handleBounce(EmailNotifyMessage message) {
        final NotifyMail mail = message.getMail();
        final Bounce bounce = message.getBounce();
        final List<String> bouncedRecipients = bounce.getStringRecipients();
        List<EmailRecipientEntity> recipientEntities = getRecipients(mail);
        recipientEntities.stream().filter(recipientEntity -> bouncedRecipients.contains(recipientEntity.getAddress())).forEach(recipientEntity -> {
            recipientEntity.setStatus(RecipientStatus.BOUNCED);
            recipientEntity.setBounced(new Date());
            db.update(recipientEntity);
        });
        eventBus.publish(EmailBounceEvent.ADDRESS, new EmailBounceEvent(message));
    }

    protected void handleComplaint(EmailNotifyMessage message) {
        final NotifyMail mail = message.getMail();
        final Complaint complaint = message.getComplaint();
        final List<String> complainedRecipients = complaint.getStringRecipients();
        List<EmailRecipientEntity> recipientEntities = getRecipients(mail);
        recipientEntities.stream().filter(recipientEntity -> complainedRecipients.contains(recipientEntity.getAddress())).forEach(recipientEntity -> {
            recipientEntity.setStatus(RecipientStatus.COMPLAINT);
            recipientEntity.setComplaint(new Date());
            db.update(recipientEntity);
        });
        eventBus.publish(EmailComplaintEvent.ADDRESS, new EmailComplaintEvent(message));
    }

    protected List<EmailRecipientEntity> getRecipients(NotifyMail mail) {
        Record record = db.select(EMAIL.fields()).from(EMAIL).where(EMAIL.MESSAGE_ID.eq(mail.getMessageId())).fetchAny();
        if(record == null) {
            LOG.error("Email Record Not Found for MessageId: " + mail.getMessageId());
            return null;
        }
        EmailEntity emailEntity = record.into(EMAIL).into(EmailEntity.class);

        List<EmailRecipientEntity> recipientEntities = db.select(EMAIL_RECIPIENT.fields()).from(EMAIL_RECIPIENT)
                .where(EMAIL_RECIPIENT.EMAIL_ID.eq(emailEntity.getId()))
                .fetch()
                .into(EMAIL_RECIPIENT)
                .into(EmailRecipientEntity.class);
        if(recipientEntities == null || recipientEntities.isEmpty()) {
            LOG.error("Email Recipients Not Found for EmailId: " + emailEntity.getId());
            return null;
        }
        return recipientEntities;
    }
}
