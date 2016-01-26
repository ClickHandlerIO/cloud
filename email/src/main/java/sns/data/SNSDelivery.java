package sns.data;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.List;

/**
 * Created by admin on 1/20/16.
 */
public class SNSDelivery {
    @JsonProperty
    private String timestamp;
    @JsonProperty
    private List<String> recipients;
    @JsonProperty
    private long processingTimeMillis;
    @JsonProperty
    private String reportingMTA;
    @JsonProperty
    private String smtpResponse;

    @JsonGetter
    public long getProcessingTimeMillis() {
        return processingTimeMillis;
    }
    @JsonSetter
    public void setProcessingTimeMillis(long processingTimeMillis) {
        this.processingTimeMillis = processingTimeMillis;
    }
    @JsonGetter
    public List<String> getRecipients() {
        return recipients;
    }
    @JsonSetter
    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
    }
    @JsonGetter
    public String getReportingMTA() {
        return reportingMTA;
    }
    @JsonSetter
    public void setReportingMTA(String reportingMTA) {
        this.reportingMTA = reportingMTA;
    }
    @JsonGetter
    public String getSmtpResponse() {
        return smtpResponse;
    }
    @JsonSetter
    public void setSmtpResponse(String smtpResponse) {
        this.smtpResponse = smtpResponse;
    }
    @JsonGetter
    public String getTimestamp() {
        return timestamp;
    }
    @JsonSetter
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
