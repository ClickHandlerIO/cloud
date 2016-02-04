package sns.data.json.email.receive;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * SNS JSON Object
 *
 * @author Brad Behnke
 */
public class DKIMVerdict {
    @JsonProperty
    private String status;

    @JsonGetter
    public String getStatus() {
        return status;
    }

    @JsonSetter
    public void setStatus(String status) {
        this.status = status;
    }
}
