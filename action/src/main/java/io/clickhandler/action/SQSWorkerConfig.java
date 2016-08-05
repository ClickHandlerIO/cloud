package io.clickhandler.action;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 */
public class SQSWorkerConfig {
    /**
     * AmazonSQS Queue URL.
     */
    @JsonProperty
    public String url;
    /**
     * Number of messages that can be "In-Flight" at a time.
     */
    @JsonProperty
    public int bufferSize = 1000;
    /**
     * Number of Send/Receive/Delete threads.
     * Each will spawn this number of threads.
     * Receive and Delete threads are only spawned the ActionManager was started in "Worker" mode.
     */
    @JsonProperty
    public int threads = 1;
    /**
     * The number of messages to use for batching.
     * AmazonSQS maxes out at 10.
     */
    @JsonProperty
    public int batchSize = 10;
    /**
     * Number of seconds for the minimum visibility timeout.
     * When pulling off of the queue the initial visibility timeout
     * is set to this number of seconds. The visibility timeout is increased
     * based on a particular Action's "maxExecutionMillis" property in it's
     * @ActionConfig annotation.
     */
    @JsonProperty
    public int minimumVisibility = 30;

    public SQSWorkerConfig url(final String url) {
        this.url = url;
        return this;
    }

    public SQSWorkerConfig bufferSize(final int bufferSize) {
        this.bufferSize = bufferSize;
        return this;
    }

    public SQSWorkerConfig threads(final int threads) {
        this.threads = threads;
        return this;
    }

    public SQSWorkerConfig batchSize(final int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    public SQSWorkerConfig minimumVisibility(final int minimumVisibility) {
        this.minimumVisibility = minimumVisibility;
        return this;
    }
}
