package io.clickhandler.action;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 */
public class SQSWorkerConfig {
    /**
     * Number of messages that can be "In-Flight" at a time.
     */
    @JsonProperty
    public int bufferSize = 1000;
    /**
     *
     */
    @JsonProperty
    public int sendThreads = 1;
    /**
     *
     */
    @JsonProperty
    public int receiveThreads = 1;
    /**
     *
     */
    @JsonProperty
    public int deleteThreads = 1;
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
     *
     * @ActionConfig annotation.
     */
    @JsonProperty
    public int minimumVisibility = 45;
    /**
     * The amount of extra visibility time to give an Action when the
     * backlog for that particular Action type is full.
     */
    @JsonProperty
    public double maxVisibilityMultiple = 2.0;

    /**
     * @param bufferSize
     * @return
     */
    public SQSWorkerConfig bufferSize(final int bufferSize) {
        this.bufferSize = bufferSize;
        return this;
    }

    /**
     * @param sendThreads
     * @return
     */
    public SQSWorkerConfig sendThreads(final int sendThreads) {
        this.sendThreads = sendThreads;
        return this;
    }

    /**
     * @param receiveThreads
     * @return
     */
    public SQSWorkerConfig receiveThreads(final int receiveThreads) {
        this.receiveThreads = receiveThreads;
        return this;
    }

    /**
     * @param deleteThreads
     * @return
     */
    public SQSWorkerConfig deleteThreads(final int deleteThreads) {
        this.deleteThreads = deleteThreads;
        return this;
    }

    /**
     * @param batchSize
     * @return
     */
    public SQSWorkerConfig batchSize(final int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    /**
     * @param minimumVisibility
     * @return
     */
    public SQSWorkerConfig minimumVisibility(final int minimumVisibility) {
        this.minimumVisibility = minimumVisibility;
        return this;
    }

    /**
     *
     * @param maxVisibilityMultiple
     * @return
     */
    public SQSWorkerConfig maxVisibilityMultiple(final double maxVisibilityMultiple) {
        this.maxVisibilityMultiple = maxVisibilityMultiple;
        return this;
    }
}
