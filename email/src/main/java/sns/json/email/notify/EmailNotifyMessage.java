package sns.json.email.notify;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import sns.json.Message;

/**
 * Created by admin on 1/20/16.
 */
public class EmailNotifyMessage extends Message {
    @JsonProperty
    private String notificationType;
    @JsonProperty
    private Bounce bounce;
    @JsonProperty
    private Complaint complaint;
    @JsonProperty
    private Delivery delivery;
    @JsonProperty
    private NotifyMail mail;

    @JsonGetter
    public Bounce getBounce() {
        return bounce;
    }
    @JsonSetter
    public void setBounce(Bounce bounce) {
        this.bounce = bounce;
    }
    @JsonGetter
    public Complaint getComplaint() {
        return complaint;
    }
    @JsonSetter
    public void setComplaint(Complaint complaint) {
        this.complaint = complaint;
    }
    @JsonGetter
    public Delivery getDelivery() {
        return delivery;
    }
    @JsonSetter
    public void setDelivery(Delivery delivery) {
        this.delivery = delivery;
    }
    @JsonGetter
    public NotifyMail getMail() {
        return mail;
    }
    @JsonSetter
    public void setMail(NotifyMail mail) {
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

