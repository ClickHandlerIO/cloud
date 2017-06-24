package move.presence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;
import java.io.IOException;
import java.util.List;
import javaslang.control.Try;

/**
 *
 */
public class Presence implements DataSerializable {

  @JsonProperty
  public String key;
  @JsonProperty
  public String mod;
  @JsonProperty
  public long seq;
  @JsonProperty
  public List<PresenceOccupant> occupants;

  @JsonIgnore
  public long lastCheck;

  public Presence key(final String key) {
    this.key = key;
    return this;
  }

  public Presence mod(final String mod) {
    this.mod = mod;
    return this;
  }

  public Presence seq(final long seq) {
    this.seq = seq;
    return this;
  }

  public Presence occupants(final List<PresenceOccupant> occupants) {
    this.occupants = occupants;
    return this;
  }

  public Presence lastCheck(final long lastCheck) {
    this.lastCheck = lastCheck;
    return this;
  }

  @Override
  public void writeData(ObjectDataOutput out) throws IOException {
    out.writeUTF(key);
    out.writeUTF(mod);
    out.writeLong(seq);
    out.writeLong(lastCheck);
    if (occupants == null || occupants.isEmpty()) {
      out.writeShort(0);
      return;
    }
    out.writeShort(occupants.size());
    occupants.forEach(occupant -> Try.run(() -> {
      occupant.writeData(out);
    }));
  }

  @Override
  public void readData(ObjectDataInput in) throws IOException {
    key = in.readUTF();
    mod = in.readUTF();
    seq = in.readLong();
    lastCheck = in.readLong();
    int size = in.readShort();
    final List<PresenceOccupant> occupants = Lists.newArrayListWithExpectedSize(size);
    for (int i = 0; i < size; i++) {
      final PresenceOccupant occupant = new PresenceOccupant();
      occupant.readData(in);
      occupants.add(occupant);
    }
    this.occupants = occupants;
  }

  @Override
  public String toString() {
    return "Presence{" +
        "key='" + key + '\'' +
        ", mod='" + mod + '\'' +
        ", seq=" + seq +
        ", occupants=" + occupants +
        '}';
  }
}
