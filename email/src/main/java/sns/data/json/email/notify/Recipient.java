package sns.data.json.email.notify;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * SNS JSON Recipient Object
 *
 * @author Brad Behnke
 */
public class Recipient {
    @JsonProperty
    private String emailAddress;
    @JsonProperty
    private String status;
    @JsonProperty
    private String action;
    @JsonProperty
    private String diagnosticCode;

    @JsonGetter
    public String getAction() {
        return action;
    }
    @JsonSetter
    public void setAction(String action) {
        this.action = action;
    }
    @JsonGetter
    public String getDiagnosticCode() {
        return diagnosticCode;
    }
    @JsonSetter
    public void setDiagnosticCode(String diagnosticCode) {
        this.diagnosticCode = diagnosticCode;
    }
    @JsonGetter
    public String getEmailAddress() {
        return emailAddress;
    }
    @JsonSetter
    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
    @JsonGetter
    public String getStatus() {
        return status;
    }
    @JsonSetter
    public void setStatus(String status) {
        this.status = status;
    }
}
