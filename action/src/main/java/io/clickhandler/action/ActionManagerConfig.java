package io.clickhandler.action;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;

import java.util.List;

/**
 *
 */
public class ActionManagerConfig {
    @JsonProperty
    public boolean worker = true;
    @JsonProperty
    public List<WorkerConfig> workerConfigs = Lists.newArrayList(
        new WorkerConfig().name(WorkerAction.DEFAULT)
    );

    public ActionManagerConfig worker(final boolean worker) {
        this.worker = worker;
        return this;
    }

    public ActionManagerConfig workerConfigs(final List<WorkerConfig> workerConfigs) {
        this.workerConfigs = workerConfigs;
        return this;
    }
}
