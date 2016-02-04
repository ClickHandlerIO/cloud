package io.clickhandler.email.sns.data.json.email.receive;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.List;

/**
 * SNS JSON Mail Object for Email Receive
 *
 * @author Brad Behnke
 */
public class ReceiveMail {
    @JsonProperty
    private List<String> destination;
    @JsonProperty
    private String messageId;
    @JsonProperty
    private String source;
    @JsonProperty
    private String timestamp;
    @JsonProperty
    private List<Header> headers;
    @JsonProperty
    private CommonHeaders commonHeaders;
    @JsonProperty
    private boolean headersTruncated;

    @JsonGetter
    public CommonHeaders getCommonHeaders() {
        return commonHeaders;
    }

    @JsonSetter
    public void setCommonHeaders(CommonHeaders commonHeaders) {
        this.commonHeaders = commonHeaders;
    }

    @JsonGetter
    public List<String> getDestination() {
        return destination;
    }

    @JsonSetter
    public void setDestination(List<String> destination) {
        this.destination = destination;
    }

    @JsonGetter
    public List<Header> getHeaders() {
        return headers;
    }

    @JsonSetter
    public void setHeaders(List<Header> headers) {
        this.headers = headers;
    }

    @JsonGetter
    public boolean isHeadersTruncated() {
        return headersTruncated;
    }

    @JsonSetter
    public void setHeadersTruncated(boolean headersTruncated) {
        this.headersTruncated = headersTruncated;
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
    public String getSource() {
        return source;
    }

    @JsonSetter
    public void setSource(String source) {
        this.source = source;
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
