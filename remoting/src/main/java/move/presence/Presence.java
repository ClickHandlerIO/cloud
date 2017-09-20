package move.presence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 *
 */
public class Presence {

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
  public String toString() {
    return "Presence{" +
        "key='" + key + '\'' +
        ", mod='" + mod + '\'' +
        ", seq=" + seq +
        ", occupants=" + occupants +
        '}';
  }
}
