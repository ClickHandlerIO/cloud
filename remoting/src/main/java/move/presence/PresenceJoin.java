package move.presence;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;
import java.io.IOException;
import java.util.Map;

/**
 *
 */
public class PresenceJoin implements DataSerializable {

  @JsonProperty
  public String sessionId;
  @JsonProperty
  public String userId;
  @JsonProperty
  public String nodeId;
  @JsonProperty
  public String name;
  @JsonProperty
  public String imageUrl;
  @JsonProperty
  public String device;
  @JsonProperty
  public String state;

  public static PresenceJoin from(ObjectDataInput in) throws IOException {
    final PresenceJoin request = new PresenceJoin();
    request.readData(in);
    return request;
  }

  public static PresenceJoin from(Map.Entry<String, PresenceOccupant> entry) {
    if (entry == null) {
      return new PresenceJoin();
    }
    return from(entry.getValue());
  }

  public static PresenceJoin from(PresenceOccupant occupant) {
    if (occupant == null) {
      return new PresenceJoin();
    }

    return new PresenceJoin()
        .sessionId(occupant.id)
        .userId(occupant.userId)
        .nodeId(occupant.nodeId)
        .name(occupant.name)
        .imageUrl(occupant.imageUrl)
        .device(occupant.device)
        .state(occupant.state);
  }

  public PresenceJoin sessionId(final String sessionId) {
    this.sessionId = sessionId;
    return this;
  }

  public PresenceJoin userId(final String userId) {
    this.userId = userId;
    return this;
  }

  public PresenceJoin nodeId(final String nodeId) {
    this.nodeId = nodeId;
    return this;
  }

  public PresenceJoin name(final String name) {
    this.name = name;
    return this;
  }

  public PresenceJoin imageUrl(final String imageUrl) {
    this.imageUrl = imageUrl;
    return this;
  }

  public PresenceJoin device(final String device) {
    this.device = device;
    return this;
  }

  public PresenceJoin state(final String state) {
    this.state = state;
    return this;
  }

  @Override
  public void writeData(ObjectDataOutput out) throws IOException {
    out.writeUTF(sessionId);
    out.writeUTF(userId);
    out.writeUTF(nodeId);
    out.writeUTF(name);
    out.writeUTF(imageUrl);
    out.writeUTF(device);
    out.writeUTF(state);
  }

  @Override
  public void readData(ObjectDataInput in) throws IOException {
    sessionId = in.readUTF();
    userId = in.readUTF();
    nodeId = in.readUTF();
    name = in.readUTF();
    imageUrl = in.readUTF();
    device = in.readUTF();
    state = in.readUTF();
  }
}
