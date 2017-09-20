package move.presence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 */
public class PresenceOccupant {

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
}
