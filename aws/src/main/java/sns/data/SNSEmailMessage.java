package sns.data;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Created by admin on 1/20/16.
 */
public class SNSEmailMessage  extends SNSMessage{
    @JsonProperty
    private String notificationType;
    @JsonProperty
    private SNSBounce bounce;
    @JsonProperty
    private SNSComplaint complaint;
    @JsonProperty
    private SNSDelivery delivery;
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

