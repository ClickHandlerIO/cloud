package io.clickhandler.action;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 */
public class SQSWorkerConfig {
    /**
     * Application queue name.
     */
    @JsonProperty
    public String name;
    /**
     * AWS SQS queue name
     */
    @JsonProperty
    public String sqsName;
    /**
     * AWS region
     */
    @JsonProperty
    public String region;
    /**
     *
     */
    @JsonProperty
    public String accessKey;
    /**
     *
     */
    @JsonProperty
    public String secretKey;
    /**
     * Number of messages that can be "In-Flight" at a time.
     */
    @JsonProperty
    public int bufferSize = 1000;
    /**
     * Number of sender threads.
     */
    @JsonProperty
    public int sendThreads = 1;
    /**
     * Number of ReceiveMessageBatch threads.
     * <p>
     * If 0, then this node will not consume messages from this queue.
     */
    @JsonProperty
    public int receiveThreads = 1;
    /**
     * Number of DeleteMessageBatch threads.
     * <p>
     * If 0, then this node will not consume messages from this queue.
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
     * ActionConfig annotation.
     */
    @JsonProperty
    public int minimumVisibility = 45;
    /**
     * Number of seconds to add to calculations regarding "ChangeMessageVisibility" calls.
     *
     * This can help alleviate / eliminate duplicate message deliveries based
     * on a race condition to delete a "processed" message before it's visibility timeout is reached.
     */
    @JsonProperty
    public int visibilityBuffer = 5;
    /**
     * The amount of extra visibility time to give an Action when the
     * backlog for that particular Action type is full.
     */
    @JsonProperty
    public double maxVisibilityMultiple = 2.0;
    /**
     * S3 Bucket.
     */
    @JsonProperty
    public String s3Bucket;
    /**
     * S3 AWS Acccess Key.
     */
    @JsonProperty
    public String s3AccessKey;
    /**
     * S3 AWS Secret Key.
     */
    @JsonProperty
    public String s3SecretKey;
    /**
     * Determines whether to use S3 for the message transport.
     * This is a way to support encrypted transport as long as
     * the S3 bucket is encrypted.
     */
    @JsonProperty
    public boolean alwaysUseS3;
    /**
     * The size in bytes that determines when to use S3 for message payload.
     * If "alwaysUseS3" is set to true, then this parameter is ignored.
     * If set to 0 then messages larger than 256kb will we stored in S3.
     * Value must be between 0 and 262000.
     */
    @JsonProperty
    public int s3MessageThreshold = 0;

    public SQSWorkerConfig name(final String name) {
        this.name = name;
        return this;
    }

    public SQSWorkerConfig sqsName(final String sqsName) {
        this.sqsName = sqsName;
        return this;
    }

    public SQSWorkerConfig region(final String region) {
        this.region = region;
        return this;
    }

    public SQSWorkerConfig accessKey(final String accessKey) {
        this.accessKey = accessKey;
        return this;
    }

    public SQSWorkerConfig secretKey(final String secretKey) {
        this.secretKey = secretKey;
        return this;
    }

    public SQSWorkerConfig bufferSize(final int bufferSize) {
        this.bufferSize = bufferSize;
        return this;
    }

    public SQSWorkerConfig sendThreads(final int sendThreads) {
        this.sendThreads = sendThreads;
        return this;
    }

    public SQSWorkerConfig receiveThreads(final int receiveThreads) {
        this.receiveThreads = receiveThreads;
        return this;
    }

    public SQSWorkerConfig deleteThreads(final int deleteThreads) {
        this.deleteThreads = deleteThreads;
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

    public SQSWorkerConfig visibilityBuffer(final int visibilityBuffer) {
        this.visibilityBuffer = visibilityBuffer;
        return this;
    }

    public SQSWorkerConfig maxVisibilityMultiple(final double maxVisibilityMultiple) {
        this.maxVisibilityMultiple = maxVisibilityMultiple;
        return this;
    }

    public SQSWorkerConfig s3Bucket(final String s3Bucket) {
        this.s3Bucket = s3Bucket;
        return this;
    }

    public SQSWorkerConfig s3AccessKey(final String s3AccessKey) {
        this.s3AccessKey = s3AccessKey;
        return this;
    }

    public SQSWorkerConfig s3SecretKey(final String s3SecretKey) {
        this.s3SecretKey = s3SecretKey;
        return this;
    }

    public SQSWorkerConfig alwaysUseS3(final boolean alwaysUseS3) {
        this.alwaysUseS3 = alwaysUseS3;
        return this;
    }

    public SQSWorkerConfig s3MessageThreshold(final int s3MessageThreshold) {
        this.s3MessageThreshold = s3MessageThreshold;
        return this;
    }
}
