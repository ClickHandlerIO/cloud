package io.clickhandler.remoting;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 */
public class WsConnectionId {
    private String nodeId;
    private long connectionId;

    public WsConnectionId() {
    }

    public WsConnectionId(String nodeId, long connectionId) {
        this.nodeId = nodeId;
        this.connectionId = connectionId;
    }

    @JsonProperty("n")
    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    @JsonProperty("w")
    public long getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(long connectionId) {
        this.connectionId = connectionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WsConnectionId that = (WsConnectionId) o;

        if (connectionId != that.connectionId) return false;
        return nodeId.equals(that.nodeId);

    }

    @Override
    public int hashCode() {
        int result = nodeId.hashCode();
        result = 31 * result + (int) (connectionId ^ (connectionId >>> 32));
        return result;
    }
}