package io.clickhandler.action;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 */
public class WorkerConfig {
    @JsonProperty
    public String name;
    @JsonProperty
    public SQSWorkerConfig sqsConfig;

    public WorkerConfig name(final String name) {
        this.name = name;
        return this;
    }

    public WorkerConfig sqsConfig(final SQSWorkerConfig sqsConfig) {
        this.sqsConfig = sqsConfig;
        return this;
    }
}
