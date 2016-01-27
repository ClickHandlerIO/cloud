package sns.data.json.email.receive;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * SNS JSON Header Object
 *
 * @author Brad Behnke
 */
public class Header {
    @JsonProperty
    private String name;
    @JsonProperty
    private String value;

    @JsonGetter
    public String getName() {
        return name;
    }

    @JsonSetter
    public void setName(String name) {
        this.name = name;
    }

    @JsonGetter
    public String getValue() {
        return value;
    }

    @JsonSetter
    public void setValue(String value) {
        this.value = value;
    }
}
