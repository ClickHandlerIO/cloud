package sns.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by admin on 1/25/16.
 */
public class SNSConfig {
    private static String name = "SNS";
    private static int parallelism = 2;
    private static int batchSize = 10;
    private static List<String> generalSubscriptionArnList;
    private static List<String> emailSubscriptionArnList;

    public SNSConfig() {
        generalSubscriptionArnList = new ArrayList<>();
        // add subscriptions
    }

    public static int getBatchSize() {
        return batchSize;
    }

    public static void setBatchSize(int batchSize) {
        SNSConfig.batchSize = batchSize;
    }

    public static List<String> getEmailSubscriptionArnList() {
        return emailSubscriptionArnList;
    }

    public static void setEmailSubscriptionArnList(List<String> emailSubscriptionArnList) {
        SNSConfig.emailSubscriptionArnList = emailSubscriptionArnList;
    }

    public static void addEmailSubcriptionArn(String arn) {
        SNSConfig.emailSubscriptionArnList.add(arn);
    }

    public static List<String> getGeneralSubscriptionArnList() {
        return generalSubscriptionArnList;
    }

    public static void setGeneralSubscriptionArnList(List<String> generalSubscriptionArnList) {
        SNSConfig.generalSubscriptionArnList = generalSubscriptionArnList;
    }

    public static void addGeneralSubcriptionArn(String arn) {
        SNSConfig.generalSubscriptionArnList.add(arn);
    }

    public static String getName() {
        return name;
    }

    public static void setName(String name) {
        SNSConfig.name = name;
    }

    public static int getParallelism() {
        return parallelism;
    }

    public static void setParallelism(int parallelism) {
        SNSConfig.parallelism = parallelism;
    }
}
