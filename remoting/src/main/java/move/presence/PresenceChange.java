package move.presence;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 *
 */
public class PresenceChange {
    @JsonProperty
    public String key;
    @JsonProperty
    public String mod;
    @JsonProperty
    public long seq;
    @JsonProperty
    public List<PresenceOccupant> joined;
    @JsonProperty
    public List<PresenceLeave> left;
    @JsonProperty
    public List<PresenceStateChanged> changed;

    public PresenceChange key(final String key) {
        this.key = key;
        return this;
    }

    public PresenceChange mod(final String mod) {
        this.mod = mod;
        return this;
    }

    public PresenceChange seq(final long seq) {
        this.seq = seq;
        return this;
    }

    public PresenceChange joined(final List<PresenceOccupant> joined) {
        this.joined = joined;
        return this;
    }

    public PresenceChange left(final List<PresenceLeave> left) {
        this.left = left;
        return this;
    }

    public PresenceChange changed(final List<PresenceStateChanged> changed) {
        this.changed = changed;
        return this;
    }
}
