package io.clickhandler.action;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 */
public class ActionManagerConfig {
    @JsonProperty
    public boolean worker;
    @JsonProperty
    public WorkerConfig[] workerConfigs;

    public ActionManagerConfig worker(final boolean worker) {
        this.worker = worker;
        return this;
    }

    public ActionManagerConfig workerConfigs(final WorkerConfig[] workerConfigs) {
        this.workerConfigs = workerConfigs;
        return this;
    }
}
