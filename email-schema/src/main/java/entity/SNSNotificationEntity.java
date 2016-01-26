package entity;

import io.clickhandler.sql.annotations.Column;
import io.clickhandler.sql.annotations.Table;
import io.clickhandler.sql.entity.AbstractEntity;
import json.SNSGeneralMessage;

import java.util.Date;

/**
 * Created by admin on 1/26/16.
 */
@Table
public class SNSNotificationEntity extends AbstractEntity {
    @Column
    private String type;
    @Column
    private String messageId;
    @Column
    private String topicArn;
    @Column
    private String subject;
    @Column
    private String message;
    @Column
    private Date date;

    public SNSNotificationEntity(SNSGeneralMessage message) {
        this.type = message.getType();
        this.messageId = message.getMessageId();
        this.topicArn = message.getTopicArn();
        this.subject = message.getSubject();
        this.message = message.getMessage();
        this.date = new Date();
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTopicArn() {
        return topicArn;
    }

    public void setTopicArn(String topicArn) {
        this.topicArn = topicArn;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
