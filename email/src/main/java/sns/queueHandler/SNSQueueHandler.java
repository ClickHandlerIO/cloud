package _engine.sns.queueHandler;

import _engine.sns.json.*;
import _engine.sns.util.SNSMessageType;
import data.Db;
import data.schema.tables.Email;
import data.schema.tables.EmailRecipient;
import io.clickhandler.queue.QueueHandler;
import io.clickhandler.sql.db.Database;
import io.clickhandler.sql.db.DatabaseSession;
import model.entity.email.EmailEntity;
import model.entity.email.EmailRecipientEntity;
import model.entity.email.RecipientStatus;
import org.jooq.Record;
import org.jooq.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author Brad Behnke
 */
@Singleton
public class SNSQueueHandler implements QueueHandler<SNSMessage> {

    public static final Logger LOG = LoggerFactory.getLogger(SNSQueueHandler.class);
    private final List<String> permanentSubscriptionTopicArns;

    private final Database db;

    @Inject
    public SNSQueueHandler(Database db) {
        this.db = db;
        this.permanentSubscriptionTopicArns = new ArrayList<>();
        // TODO add subscriptions that should  not be unsubscribed.
    }

    @Override
    public void receive(List<SNSMessage> messages) {
        messages.forEach(this::handle);
    }

    private void handle(SNSMessage message) {
        if(message instanceof SNSGeneralMessage) {
            handleMessage((SNSGeneralMessage) message);
        }
        if(message instanceof SNSEmailMessage) {
            handleMessage((SNSEmailMessage) message);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Notification, Subscribe, and Unsubscribe Handling
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void handleMessage(SNSGeneralMessage message) {
        if(message == null || message.getType() == null) {
            LOG.error("Invalid SNSGeneralMessage Received");
            return;
        }
        LOG.info("SNS General Message Received: " + message.getType());
        switch (SNSMessageType.getTypeEnum(message.getType())) {
            case SUB_CONFIRM:
                handleSubscribe(message);
                break;
            case UNSUB_CONFIRM:
                handleUnsubscribe(message);
                break;
            case NOTIFICATION:
                handleNotification(message);
                break;
            default:
                break;
        }
    }

    private void handleSubscribe(SNSGeneralMessage message) {
        try {
            // TODO check topic ARN is one we want to be subscribed to.
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

    private void handleUnsubscribe(SNSGeneralMessage message) {
        try {
            // make sure topic ARN is not a permanent one, if it is resubscribe to it, if not confirm unsubscribe.
            String urlString = isUnsubAllowed(message.getTopicArn()) ? message.getUnsubscribeURL():message.getSubscribeURL();
            // open subscription URL
            Scanner sc = new Scanner(new URL(urlString).openStream());
            // Capture XML body returned.
            StringBuilder sb = new StringBuilder();
            while (sc.hasNextLine()) {
                sb.append(sc.nextLine());
            }

            // TODO ensure XML body reflects successful unsubcribe or subscribe.
        } catch (Exception e) {
            LOG.error("Failed to confirm subscription to topic ARN: " + message.getTopicArn());
        }
    }

    private boolean isUnsubAllowed(String topicArn) {
        return !permanentSubscriptionTopicArns.contains(topicArn);
    }

    private void handleNotification(SNSGeneralMessage message) {
        // TODO
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Email Status Notification Handling
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void handleMessage(SNSEmailMessage message) {
        if(message == null || message.getNotificationType() == null) {
            LOG.error("Invalid SNSEmailMessage Received");
            return;
        }
        LOG.info("SNS Email Message Received: " + message.getNotificationType());
        switch (SNSMessageType.getTypeEnum(message.getNotificationType())) {
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
                break;
        }
    }

    private void handleDelivery(SNSEmailMessage message) {
        final SNSMail mail = message.getMail();
        final SNSDelivery delivery = message.getDelivery();
        // TODO
//        db.execute(sql -> {
//            List<EmailRecipientEntity> recipientEntities = getRecipients(mail, sql);
//            if(recipientEntities == null) {
//                return;
//            }
//            recipientEntities.stream().filter(recipientEntity -> delivery.getRecipients().contains(recipientEntity.getAddress())).forEach(recipientEntity -> {
//                recipientEntity.setStatus(RecipientStatus.DELIVERED);
//                recipientEntity.setDelivered(new Date());
//                sql.update(recipientEntity);
//            });
//        });
    }

    private void handleBounce(SNSEmailMessage message) {
        final SNSMail mail = message.getMail();
        final SNSBounce bounce = message.getBounce();
        final List<String> bouncedRecipients = bounce.getStringRecipients();
        // TODO
//        db.execute(sql -> {«
//            List<EmailRecipientEntity> recipientEntities = getRecipients(mail, sql);
//            if(recipientEntities == null) {
//                return;
//            }
//            recipientEntities.stream().filter(recipientEntity -> bouncedRecipients.contains(recipientEntity.getAddress())).forEach(recipientEntity -> {
//                recipientEntity.setStatus(RecipientStatus.BOUNCED);
//                recipientEntity.setBounced(new Date());
//                sql.update(recipientEntity);
//            });
//        });
    }

    private void handleComplaint(SNSEmailMessage message) {
        final SNSMail mail = message.getMail();
        final SNSComplaint complaint = message.getComplaint();
        final List<String> complainedRecipients = complaint.getStringRecipients();
        // TODO
//        db.execute(sql -> {
//            List<EmailRecipientEntity> recipientEntities = getRecipients(mail, sql);
//            if(recipientEntities == null) {
//                return;
//            }
//            recipientEntities.stream().filter(recipientEntity -> complainedRecipients.contains(recipientEntity.getAddress())).forEach(recipientEntity -> {
//                recipientEntity.setStatus(RecipientStatus.COMPLAINT);
//                recipientEntity.setComplaint(new Date());
//                sql.update(recipientEntity);
//            });
//        });
    }

    private List<EmailRecipientEntity> getRecipients(SNSMail mail, DatabaseSession sql) {
        // TODO
//        Result<Record> result = sql.select(Email.EMAIL.fields()).from(Email.EMAIL)
//                .where(Email.EMAIL.SES_MESSAGE_ID.eq(mail.getMessageId()))
//                .getResult();
//        if(result == null || result.isEmpty()) {
//            LOG.error("Email Entity Fetch for SNS Delivery Failed.");
//            return null;
//        }
//        List<EmailEntity> emailEntities = result.into(Email.EMAIL).into(EmailEntity.class);
//        EmailEntity emailEntity = emailEntities.get(0);
//        if(emailEntity == null) {
//            LOG.error("Email Entity Fetch for SNS Delivery Failed.");
//            return null;
//        }
//        result = sql.select(EmailRecipient.EMAIL_RECIPIENT.fields())
//                .from(EmailRecipient.EMAIL_RECIPIENT)
//                .where(EmailRecipient.EMAIL_RECIPIENT.EMAIL_ID.eq(emailEntity.getId()))
//                .getResult();
//
//        if(result == null || result.isEmpty()) {
//            LOG.error("Recipient Fetch for SNS Delivery Failed.");
//            return null;
//        }
//        List<EmailRecipientEntity> recipientEntities = result.into(EmailRecipient.EMAIL_RECIPIENT).into(EmailRecipientEntity.class);
//        if(recipientEntities == null || recipientEntities.isEmpty()) {
//            LOG.error("Recipient Fetch for SNS Delivery Failed.");
//            return null;
//        }
//        if(recipientEntities.isEmpty()) {
//            LOG.error("Recipient Fetch for SNS Delivery Failed.");
//            return null;
//        }
//        return recipientEntities;
        return null;
    }
}
