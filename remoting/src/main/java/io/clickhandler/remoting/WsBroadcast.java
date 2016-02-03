package io.clickhandler.remoting;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Set;

/**
 *
 */
public class WsBroadcast {
    private Set<String> userIds;
    private WsEnvelope envelope;

    public WsBroadcast() {
    }

    public WsBroadcast(Set<String> userIds, WsEnvelope envelope) {
        this.userIds = userIds;
        this.envelope = envelope;
    }

    @JsonProperty("u")
    public Set<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(Set<String> userIds) {
        this.userIds = userIds;
    }

    @JsonProperty("e")
    public WsEnvelope getEnvelope() {
        return envelope;
    }

    public void setEnvelope(WsEnvelope envelope) {
        this.envelope = envelope;
    }
}