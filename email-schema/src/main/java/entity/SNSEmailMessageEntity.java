package entity;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.clickhandler.sql.annotations.Column;
import io.clickhandler.sql.annotations.Table;

/**
 * Created by admin on 1/20/16.
 */
@Table
public class SNSEmailMessageEntity extends SNSMessageEntity {
    @Column
    @JsonProperty
    private String notificationType;
    @Column
    @JsonProperty
    private SNSBounce bounce;
    @Column
    @JsonProperty
    private SNSComplaint complaint;
    @Column
    @JsonProperty
    private SNSDelivery delivery;
    @Column
    @JsonProperty
    private SNSMail mail;

    @JsonGetter
    public SNSBounce getBounce() {
        return bounce;
    }
    @JsonSetter
    public void setBounce(SNSBounce bounce) {
        this.bounce = bounce;
    }
    @JsonGetter
    public SNSComplaint getComplaint() {
        return complaint;
    }
    @JsonSetter
    public void setComplaint(SNSComplaint complaint) {
        this.complaint = complaint;
    }
    @JsonGetter
    public SNSDelivery getDelivery() {
        return delivery;
    }
    @JsonSetter
    public void setDelivery(SNSDelivery delivery) {
        this.delivery = delivery;
    }
    @JsonGetter
    public SNSMail getMail() {
        return mail;
    }
    @JsonSetter
    public void setMail(SNSMail mail) {
        this.mail = mail;
    }
    @JsonGetter
    public String getNotificationType() {
        return notificationType;
    }
    @JsonSetter
    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }
}

