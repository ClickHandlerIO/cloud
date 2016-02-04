package io.clickhandler.email.ses.config;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import io.clickhandler.email.common.config.EmailConfig;

/**
 *  Configuration settings for SES email service.
 *
 *  @author Brad Behnke
 */
public class SESConfig extends EmailConfig {
    private String awsAccessKey;
    private String awsSecretKey;
    private Region awsRegion = Region.getRegion(Regions.US_WEST_2);
    private int sendParallelism = 2;
    private int sendBatchSize = 10;
    private int sendRetryMax = 3;

    public SESConfig() {
    }

    public String getAwsAccessKey() {
        return awsAccessKey;
    }

    public SESConfig awsAccessKey(String awsAccessKey) {
        this.awsAccessKey = awsAccessKey;
        return this;
    }

    public Region getAwsRegion() {
        return awsRegion;
    }

    public SESConfig awsRegion(Region awsRegion) {
        this.awsRegion = awsRegion;
        return this;
    }

    public String getAwsSecretKey() {
        return awsSecretKey;
    }

    public SESConfig awsSecretKey(String awsSecretKey) {
        this.awsSecretKey = awsSecretKey;
        return this;
    }

    public int getSendBatchSize() {
        return sendBatchSize;
    }

    public SESConfig sendBatchSize(int sendBatchSize) {
        this.sendBatchSize = sendBatchSize;
        return this;
    }

    public int getSendParallelism() {
        return sendParallelism;
    }

    public SESConfig sendParallelism(int sendParallelism) {
        this.sendParallelism = sendParallelism;
        return this;
    }

    public int getSendRetryMax() {
        return sendRetryMax;
    }

    public SESConfig sendRetryMax(int sendRetryMax) {
        this.sendRetryMax = sendRetryMax;
        return this;
    }
}
