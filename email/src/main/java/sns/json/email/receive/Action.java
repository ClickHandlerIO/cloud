package sns.json.email.receive;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Created by admin on 1/26/16.
 */
public class Action {
    @JsonProperty
    private String type;
    @JsonProperty
    private String topicArn;
    @JsonProperty
    private String bucketName;
    @JsonProperty
    private String objectKey;
    @JsonProperty
    private String smtpReplyCode;
    @JsonProperty
    private String statusCode;
    @JsonProperty
    private String message;
    @JsonProperty
    private String sender;
    @JsonProperty
    private String functionArn;
    @JsonProperty
    private String invocationType;
    @JsonProperty
    private String organizationArn;

    @JsonGetter
    public String getBucketName() {
        return bucketName;
    }

    @JsonSetter
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    @JsonGetter
    public String getFunctionArn() {
        return functionArn;
    }

    @JsonSetter
    public void setFunctionArn(String functionArn) {
        this.functionArn = functionArn;
    }

    @JsonGetter
    public String getInvocationType() {
        return invocationType;
    }

    @JsonSetter
    public void setInvocationType(String invocationType) {
        this.invocationType = invocationType;
    }

    @JsonGetter
    public String getMessage() {
        return message;
    }

    @JsonSetter
    public void setMessage(String message) {
        this.message = message;
    }

    @JsonGetter
    public String getObjectKey() {
        return objectKey;
    }

    @JsonSetter
    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    @JsonGetter
    public String getOrganizationArn() {
        return organizationArn;
    }

    @JsonSetter
    public void setOrganizationArn(String organizationArn) {
        this.organizationArn = organizationArn;
    }

    @JsonGetter
    public String getSender() {
        return sender;
    }

    @JsonSetter
    public void setSender(String sender) {
        this.sender = sender;
    }

    @JsonGetter
    public String getSmtpReplyCode() {
        return smtpReplyCode;
    }

    @JsonSetter
    public void setSmtpReplyCode(String smtpReplyCode) {
        this.smtpReplyCode = smtpReplyCode;
    }

    @JsonGetter
    public String getStatusCode() {
        return statusCode;
    }

    @JsonSetter
    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    @JsonGetter
    public String getTopicArn() {
        return topicArn;
    }

    @JsonSetter
    public void setTopicArn(String topicArn) {
        this.topicArn = topicArn;
    }

    @JsonGetter
    public String getType() {
        return type;
    }

    @JsonSetter
    public void setType(String type) {
        this.type = type;
    }
}
