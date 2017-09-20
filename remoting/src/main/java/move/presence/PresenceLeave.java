package move.presence;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 */
public class PresenceLeave {

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

}
