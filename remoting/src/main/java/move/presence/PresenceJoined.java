package move.presence;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 */
public class PresenceJoined {

  @JsonProperty
  public Presence presence;
  @JsonProperty
  public PresenceOccupant me;

  public PresenceJoined presence(final Presence presence) {
    this.presence = presence;
    return this;
  }

  public PresenceJoined me(final PresenceOccupant me) {
    this.me = me;
    return this;
  }
}
