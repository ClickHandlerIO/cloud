package move.presence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;

import java.io.IOException;

/**
 *
 */
public class PresenceOccupant implements DataSerializable {
    @JsonProperty
    public String userId;
    @JsonProperty
    public String id;
    @JsonProperty
    public String name;
    @JsonProperty
    public String imageUrl;
    @JsonProperty
    public String nodeId;
    @JsonProperty
    public long joined;
    @JsonProperty
    public String device;
    @JsonProperty
    public String state;

    @JsonIgnore
    public long lastPing;

    public PresenceOccupant id(final String id) {
        this.id = id;
        return this;
    }

    public PresenceOccupant userId(final String userId) {
        this.userId = userId;
        return this;
    }

    public PresenceOccupant name(final String name) {
        this.name = name;
        return this;
    }

    public PresenceOccupant imageUrl(final String imageUrl) {
        this.imageUrl = imageUrl;
        return this;
    }

    public PresenceOccupant nodeId(final String nodeId) {
        this.nodeId = nodeId;
        return this;
    }

    public PresenceOccupant joined(final long joined) {
        this.joined = joined;
        return this;
    }

    public PresenceOccupant device(final String device) {
        this.device = device;
        return this;
    }

    public PresenceOccupant state(final String state) {
        this.state = state;
        return this;
    }

    public PresenceOccupant lastPing(final long lastKeepAlive) {
        this.lastPing = lastKeepAlive;
        return this;
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeUTF(id);
        out.writeUTF(userId);
        out.writeUTF(name);
        out.writeUTF(imageUrl);
        out.writeUTF(nodeId);
        out.writeLong(joined);
        out.writeUTF(device);
        out.writeUTF(state);
        out.writeLong(lastPing);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        id = in.readUTF();
        userId = in.readUTF();
        name = in.readUTF();
        imageUrl = in.readUTF();
        nodeId = in.readUTF();
        joined = in.readLong();
        device = in.readUTF();
        state = in.readUTF();
        lastPing = in.readLong();
    }
}
