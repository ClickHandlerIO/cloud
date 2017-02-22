package move.action;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 *
 */
public class SQSConfig {
    @JsonProperty
    public List<SQSWorkerConfig> workers;

    public SQSConfig workers(final List<SQSWorkerConfig> workers) {
        this.workers = workers;
        return this;
    }
}
