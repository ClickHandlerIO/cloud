package move.presence;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;

import java.io.IOException;

/**
 *
 */
public class PresenceLeave implements DataSerializable {
    @JsonProperty
    public String id;
    @JsonProperty
    public String userId;

    public PresenceLeave id(final String sessionId) {
        this.id = sessionId;
        return this;
    }

    public PresenceLeave userId(final String userId) {
        this.userId = userId;
        return this;
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeUTF(userId);
        out.writeUTF(id);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        userId = in.readUTF();
        id = in.readUTF();
    }
}
