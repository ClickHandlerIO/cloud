package sns.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by admin on 1/25/16.
 */
public class SNSConfig {
    private String name = "SNS";
    private int parallelism = 2;
    private int batchSize = 10;
    private List<String> generalSubscriptionArnList;
    private List<String> emailSubscriptionArnList;

    public SNSConfig() {
        generalSubscriptionArnList = new ArrayList<>();
        // add subscriptions
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getParallelism() {
        return parallelism;
    }

    public void setParallelism(int parallelism) {
        this.parallelism = parallelism;
    }

    public List<String> getGeneralSubscriptionArnList() {
        return generalSubscriptionArnList;
    }

    public void setGeneralSubscriptionArnList(List<String> generalSubscriptionArnList) {
        this.generalSubscriptionArnList = generalSubscriptionArnList;
    }

    public List<String> getEmailSubscriptionArnList() {
        return emailSubscriptionArnList;
    }

    public void setEmailSubscriptionArnList(List<String> emailSubscriptionArnList) {
        this.emailSubscriptionArnList = emailSubscriptionArnList;
    }
}
