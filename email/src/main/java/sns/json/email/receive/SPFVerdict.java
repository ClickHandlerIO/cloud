package sns.json.email.receive;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Created by admin on 1/26/16.
 */
public class SPFVerdict {
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
