package move.presence;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 */
public class PresenceStateChanged {

  @JsonProperty
  public String id;
  @JsonProperty
  public String from;
  @JsonProperty
  public String to;

  public PresenceStateChanged id(final String id) {
    this.id = id;
    return this;
  }

  public PresenceStateChanged from(final String from) {
    this.from = from;
    return this;
  }

  public PresenceStateChanged to(final String state) {
    this.to = state;
    return this;
  }
}
