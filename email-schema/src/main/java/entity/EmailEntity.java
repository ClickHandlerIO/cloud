package entity;

import io.clickhandler.sql.annotations.Column;
import io.clickhandler.sql.annotations.Table;
import io.clickhandler.sql.entity.AbstractEntity;

/**
 *
 */
@Table
public class EmailEntity extends AbstractEntity {
    // Sending user Id
    @Column
    private String userId;
    @Column
    private String to;
    @Column
    private String cc;
    @Column
    private String from;
    @Column
    private String replyTo;
    @Column
    private String subject;
    @Column
    private String textBody;
    @Column
    private String htmlBody;
    @Column
    private boolean attachments;
    // Id provided from email client on send
    @Column
    private String messageId;

    public EmailEntity() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isAttachments() {
        return attachments;
    }

    public void setAttachments(boolean attachments) {
        this.attachments = attachments;
    }

    public String getCc() {
        return cc;
    }

    public void setCc(String cc) {
        this.cc = cc;
    }

    public String getHtmlBody() {
        return htmlBody;
    }

    public void setHtmlBody(String htmlBody) {
        this.htmlBody = htmlBody;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTextBody() {
        return textBody;
    }

    public void setTextBody(String textBody) {
        this.textBody = textBody;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public enum Status {
        PENDING,
        SENT,
        FAILED,
        DELIVERED,
        BOUNCED,
        COMPLAINT
    }
}
