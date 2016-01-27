package sns.data.json.email.receive;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * SNS JSON Receipt Object
 *
 * @author Brad Behnke
 */
public class Receipt {
    @JsonProperty
    private Action action;
    @JsonProperty
    private DKIMVerdict dkimVerdict;
    @JsonProperty
    private String processingTimeMillis;
    @JsonProperty
    private SpamVerdict spamVerdict;
    @JsonProperty
    private SPFVerdict spfVerdict;
    @JsonProperty
    private String timestamp;
    @JsonProperty
    private VirusVerdict virusVerdict;

    @JsonGetter
    public Action getAction() {
        return action;
    }

    @JsonSetter
    public void setAction(Action action) {
        this.action = action;
    }

    @JsonGetter
    public DKIMVerdict getDkimVerdict() {
        return dkimVerdict;
    }

    @JsonSetter
    public void setDkimVerdict(DKIMVerdict dkimVerdict) {
        this.dkimVerdict = dkimVerdict;
    }

    @JsonGetter
    public String getProcessingTimeMillis() {
        return processingTimeMillis;
    }

    @JsonSetter
    public void setProcessingTimeMillis(String processingTimeMillis) {
        this.processingTimeMillis = processingTimeMillis;
    }

    @JsonGetter
    public SpamVerdict getSpamVerdict() {
        return spamVerdict;
    }

    @JsonSetter
    public void setSpamVerdict(SpamVerdict spamVerdict) {
        this.spamVerdict = spamVerdict;
    }

    @JsonGetter
    public SPFVerdict getSpfVerdict() {
        return spfVerdict;
    }

    @JsonSetter
    public void setSpfVerdict(SPFVerdict spfVerdict) {
        this.spfVerdict = spfVerdict;
    }

    @JsonGetter
    public String getTimestamp() {
        return timestamp;
    }

    @JsonSetter
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @JsonGetter
    public VirusVerdict getVirusVerdict() {
        return virusVerdict;
    }

    @JsonSetter
    public void setVirusVerdict(VirusVerdict virusVerdict) {
        this.virusVerdict = virusVerdict;
    }
}
