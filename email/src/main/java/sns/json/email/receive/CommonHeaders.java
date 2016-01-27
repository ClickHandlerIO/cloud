package sns.json.email.receive;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.List;

/**
 * Created by admin on 1/26/16.
 */
public class CommonHeaders {
    @JsonProperty
    private String returnPath;
    @JsonProperty
    private List<String> from;
    @JsonProperty
    private String date;
    @JsonProperty
    private List<String> to;
    @JsonProperty
    private String messageId;
    @JsonProperty
    private String subject;

    @JsonGetter
    public String getDate() {
        return date;
    }

    @JsonSetter
    public void setDate(String date) {
        this.date = date;
    }

    @JsonGetter
    public List<String> getFrom() {
        return from;
    }

    @JsonSetter
    public void setFrom(List<String> from) {
        this.from = from;
    }

    @JsonGetter
    public String getMessageId() {
        return messageId;
    }

    @JsonSetter
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    @JsonGetter
    public String getReturnPath() {
        return returnPath;
    }

    @JsonSetter
    public void setReturnPath(String returnPath) {
        this.returnPath = returnPath;
    }

    @JsonGetter
    public String getSubject() {
        return subject;
    }

    @JsonSetter
    public void setSubject(String subject) {
        this.subject = subject;
    }

    @JsonGetter
    public List<String> getTo() {
        return to;
    }

    @JsonSetter
    public void setTo(List<String> to) {
        this.to = to;
    }
}
