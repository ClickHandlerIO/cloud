package sns.data.json.general;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * SNS Top-Level JSON Notification Object
 *
 * @author Brad Behnke
 */
public class GeneralMessage extends common.data.Message {
    
    @JsonProperty
    private String Type;
    @JsonProperty
    private String MessageId;
    @JsonProperty
    private String Token;
    @JsonProperty
    private String TopicArn;
    @JsonProperty
    private String Subject;
    @JsonProperty
    private String Message;
    @JsonProperty
    private String SubscribeURL;
    @JsonProperty
    private String UnsubscribeURL;
    @JsonProperty
    private String Timestamp;
    @JsonProperty
    private String SignatureVersion;
    @JsonProperty
    private String Signature;
    @JsonProperty
    private String SigningCertURL;

    @JsonGetter
    public String getMessage() {
        return Message;
    }
    @JsonSetter
    public void setMessage(String message) {
        Message = message;
    }
    @JsonGetter
    public String getMessageId() {
        return MessageId;
    }
    @JsonSetter
    public void setMessageId(String messageId) {
        MessageId = messageId;
    }
    @JsonGetter
    public String getSignature() {
        return Signature;
    }
    @JsonSetter
    public void setSignature(String signature) {
        Signature = signature;
    }
    @JsonGetter
    public String getSignatureVersion() {
        return SignatureVersion;
    }
    @JsonSetter
    public void setSignatureVersion(String signatureVersion) {
        SignatureVersion = signatureVersion;
    }
    @JsonGetter
    public String getSigningCertURL() {
        return SigningCertURL;
    }
    @JsonSetter
    public void setSigningCertURL(String signingCertURL) {
        SigningCertURL = signingCertURL;
    }
    @JsonGetter
    public String getSubject() {
        return Subject;
    }
    @JsonSetter
    public void setSubject(String subject) {
        Subject = subject;
    }
    @JsonGetter
    public String getSubscribeURL() {
        return SubscribeURL;
    }
    @JsonSetter
    public void setSubscribeURL(String subscribeURL) {
        SubscribeURL = subscribeURL;
    }
    @JsonGetter
    public String getTimestamp() {
        return Timestamp;
    }
    @JsonSetter
    public void setTimestamp(String timestamp) {
        Timestamp = timestamp;
    }
    @JsonGetter
    public String getToken() {
        return Token;
    }
    @JsonSetter
    public void setToken(String token) {
        Token = token;
    }
    @JsonGetter
    public String getTopicArn() {
        return TopicArn;
    }
    @JsonSetter
    public void setTopicArn(String topicArn) {
        TopicArn = topicArn;
    }
    @JsonGetter
    public String getType() {
        return Type;
    }
    @JsonSetter
    public void setType(String type) {
        Type = type;
    }
    @JsonGetter
    public String getUnsubscribeURL() {
        return UnsubscribeURL;
    }
    @JsonSetter
    public void setUnsubscribeURL(String unsubscribeURL) {
        UnsubscribeURL = unsubscribeURL;
    }
}
