package sns.json.email.notify;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by admin on 1/20/16.
 */
public class Complaint {
    @JsonProperty
    private String userAgent;
    @JsonProperty
    private List<Recipient> complainedRecipients;
    @JsonProperty
    private String complaintFeedbackType;
    @JsonProperty
    private String arrivalDate;
    @JsonProperty
    private String timestamp;
    @JsonProperty
    private String feedbackId;

    @JsonGetter
    public String getArrivalDate() {
        return arrivalDate;
    }
    @JsonSetter
    public void setArrivalDate(String arrivalDate) {
        this.arrivalDate = arrivalDate;
    }
    @JsonGetter
    public List<Recipient> getComplainedRecipients() {
        return complainedRecipients;
    }
    @JsonSetter
    public void setComplainedRecipients(List<Recipient> complainedRecipients) {
        this.complainedRecipients = complainedRecipients;
    }
    @JsonGetter
    public String getComplaintFeedbackType() {
        return complaintFeedbackType;
    }
    @JsonSetter
    public void setComplaintFeedbackType(String complaintFeedbackType) {
        this.complaintFeedbackType = complaintFeedbackType;
    }
    @JsonGetter
    public String getFeedbackId() {
        return feedbackId;
    }
    @JsonSetter
    public void setFeedbackId(String feedbackId) {
        this.feedbackId = feedbackId;
    }
    @JsonGetter
    public String getTimestamp() {
        return timestamp;
    }
    @JsonSetter
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    @JsonGetter
    public String getUserAgent() {
        return userAgent;
    }
    @JsonSetter
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    @JsonIgnore
    public List<String> getStringRecipients(){
        return complainedRecipients.stream().map(Recipient::getEmailAddress).collect(Collectors.toList());
    }
}
