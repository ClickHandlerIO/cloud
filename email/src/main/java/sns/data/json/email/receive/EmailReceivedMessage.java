package sns.data.json.email.receive;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import sns.data.json.common.Message;

/**
 * Created by admin on 1/26/16.
 */
public class EmailReceivedMessage extends Message {
    @JsonProperty
    private String notificationType;
    @JsonProperty
    private Receipt receipt;
    @JsonProperty
    private ReceiveMail mail;
    @JsonProperty
    private String content;

    @JsonGetter
    public ReceiveMail getMail() {
        return mail;
    }

    @JsonSetter
    public void setMail(ReceiveMail mail) {
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

    @JsonGetter
    public Receipt getReceipt() {
        return receipt;
    }

    @JsonSetter
    public void setReceipt(Receipt receipt) {
        this.receipt = receipt;
    }

    @JsonGetter
    public String getContent() {
        return content;
    }

    @JsonSetter
    public void setContent(String content) {
        this.content = content;
    }
}
