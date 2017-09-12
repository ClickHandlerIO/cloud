package move.remoting;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

/**
 *
 */
public class PushEnvelope implements Serializable {

  @JsonProperty("a")
  private String address;
  @JsonProperty("p")
  private String payload;

  public PushEnvelope() {
  }

  public String address() {
    return this.address;
  }

  public String payload() {
    return this.payload;
  }

  public PushEnvelope address(final String name) {
    this.address = name;
    return this;
  }

  public PushEnvelope payload(final String payload) {
    this.payload = payload;
    return this;
  }
}