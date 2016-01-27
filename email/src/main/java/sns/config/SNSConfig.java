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
    private static List<String> emailNotifySubscriptionArnList;
    private static List<String> emailReceivedSubscriptionArnList;

    public SNSConfig() {
        generalSubscriptionArnList = new ArrayList<>();
        emailNotifySubscriptionArnList = new ArrayList<>();
        emailReceivedSubscriptionArnList = new ArrayList<>();
        // add subscriptions
    }

    public static int getBatchSize() {
        return batchSize;
    }

    public static void setBatchSize(int batchSize) {
        SNSConfig.batchSize = batchSize;
    }

    public static List<String> getEmailNotifySubscriptionArnList() {
        return emailNotifySubscriptionArnList;
    }

    public static void setEmailNotifySubscriptionArnList(List<String> emailNotifySubscriptionArnList) {
        SNSConfig.emailNotifySubscriptionArnList = emailNotifySubscriptionArnList;
    }

    public static List<String> getEmailReceivedSubscriptionArnList() {
        return emailReceivedSubscriptionArnList;
    }

    public static void setEmailReceivedSubscriptionArnList(List<String> emailReceivedSubscriptionArnList) {
        SNSConfig.emailReceivedSubscriptionArnList = emailReceivedSubscriptionArnList;
    }

    public static void addEmailSubcriptionArn(String arn) {
        SNSConfig.emailNotifySubscriptionArnList.add(arn);
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
