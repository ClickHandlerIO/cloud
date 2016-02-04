package io.clickhandler.email.sns.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration settings for SNS service.
 *
 * @author Brad Behnke
 */
public class SNSConfig {
    private String name = "SNS";
    private int parallelism = 2;
    private int batchSize = 10;
    private List<String> generalSubscriptionArnList;
    private List<String> emailNotifySubscriptionArnList;
    private List<String> emailReceivedSubscriptionArnList;

    public SNSConfig() {
        generalSubscriptionArnList = new ArrayList<>();
        emailNotifySubscriptionArnList = new ArrayList<>();
        emailReceivedSubscriptionArnList = new ArrayList<>();
        // add subscriptions
    }

    public int getBatchSize() {
        return batchSize;
    }

    public SNSConfig batchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    public List<String> getEmailNotifySubscriptionArnList() {
        return emailNotifySubscriptionArnList;
    }

    public SNSConfig emailNotifySubscriptionArnList(List<String> emailNotifySubscriptionArnList) {
        this.emailNotifySubscriptionArnList = emailNotifySubscriptionArnList;
        return this;
    }

    public List<String> getEmailReceivedSubscriptionArnList() {
        return emailReceivedSubscriptionArnList;
    }

    public SNSConfig emailReceivedSubscriptionArnList(List<String> emailReceivedSubscriptionArnList) {
        this.emailReceivedSubscriptionArnList = emailReceivedSubscriptionArnList;
        return this;
    }

    public List<String> getGeneralSubscriptionArnList() {
        return generalSubscriptionArnList;
    }

    public SNSConfig generalSubscriptionArnList(List<String> generalSubscriptionArnList) {
        this.generalSubscriptionArnList = generalSubscriptionArnList;
        return this;
    }

    public String getName() {
        return name;
    }

    public SNSConfig name(String name) {
        this.name = name;
        return this;
    }

    public int getParallelism() {
        return parallelism;
    }

    public SNSConfig parallelism(int parallelism) {
        this.parallelism = parallelism;
        return this;
    }
}
