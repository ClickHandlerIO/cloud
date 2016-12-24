package move.presence;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

/**
 *
 */
public class PresenceMessage {
    public static final String CHANGED = "PresenceChanged";
    public static final String JOINED = "PresenceJoined";
    public static final String REMOVED = "PresenceRemoved";
    public static final String PUSH = "PresencePush";

    @JsonProperty
    public String key;
    @JsonProperty
    public Set<String> sessionIds;
    @JsonProperty
    public Presence presence;
    @JsonProperty
    public PresenceChange change;
    @JsonProperty
    public byte[] pushEnvelope;

    public PresenceMessage key(final String key) {
        this.key = key;
        return this;
    }

    public PresenceMessage sessionIds(final Set<String> sessionIds) {
        this.sessionIds = sessionIds;
        return this;
    }

    public PresenceMessage presence(final Presence presence) {
        this.presence = presence;
        return this;
    }

    public PresenceMessage change(final PresenceChange change) {
        this.change = change;
        return this;
    }

    public PresenceMessage pushEnvelope(final byte[] pushEnvelope) {
        this.pushEnvelope = pushEnvelope;
        return this;
    }
}
