package entity;

import io.clickhandler.sql.annotations.Column;
import io.clickhandler.sql.annotations.Table;
import io.clickhandler.sql.entity.AbstractEntity;

import java.util.Date;

/**
 *
 */
@Table
public class EmailRecipientEntity extends AbstractEntity {
    @Column
    private String emailId;
    @Column
    private RecipientType type;
    @Column
    private String name;
    @Column
    private String address;
    @Column
    private RecipientStatus status;
    @Column
    private Date sent;
    @Column
    private Date delivered;
    @Column
    private Date bounced;
    @Column
    private Date complaint;
    @Column
    private Date failed;
    @Column
    private Date opened;
    @Column
    private String contactId;
    @Column
    private String userId;

    public Date getComplaint() {
        return complaint;
    }

    public void setComplaint(Date complaint) {
        this.complaint = complaint;
    }

    public String getEmailId() {
        return emailId;
    }

    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }

    public RecipientType getType() {
        return type;
    }

    public void setType(RecipientType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public RecipientStatus getStatus() {
        return status;
    }

    public void setStatus(RecipientStatus status) {
        this.status = status;
    }

    public Date getSent() {
        return sent;
    }

    public void setSent(Date sent) {
        this.sent = sent;
    }

    public Date getDelivered() {
        return delivered;
    }

    public void setDelivered(Date delivered) {
        this.delivered = delivered;
    }

    public Date getBounced() {
        return bounced;
    }

    public void setBounced(Date bounced) {
        this.bounced = bounced;
    }

    public Date getFailed() {
        return failed;
    }

    public void setFailed(Date failed) {
        this.failed = failed;
    }

    public Date getOpened() {
        return opened;
    }

    public void setOpened(Date opened) {
        this.opened = opened;
    }

    public String getContactId() {
        return contactId;
    }

    public void setContactId(String contactId) {
        this.contactId = contactId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
