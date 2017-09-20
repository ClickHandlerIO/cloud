package move.presence;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 */
public class PresenceJoin {

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
}
