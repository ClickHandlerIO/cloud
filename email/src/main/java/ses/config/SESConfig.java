package ses.config;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

/**
 * Created by admin on 1/25/16.
 */
public class SESConfig {
    private static String name = "SES";
    private static String awsAccessKey;
    private static String awsSecretKey;
    private static Region awsRegion = Region.getRegion(Regions.US_WEST_2);
    private static int sendParallelism = 2;
    private static int sendBatchSize = 10;
    private static int prepParallelism = 2;
    private static int prepBatchSize = 10;
    private static int attachmentParallelism = 2;
    private static int attachmentBatchSize = 10;
    private static int sendRetryMax = 3;

    public static int getAttachmentBatchSize() {
        return attachmentBatchSize;
    }

    public static void setAttachmentBatchSize(int attachmentBatchSize) {
        SESConfig.attachmentBatchSize = attachmentBatchSize;
    }

    public static int getAttachmentParallelism() {
        return attachmentParallelism;
    }

    public static void setAttachmentParallelism(int attachmentParallelism) {
        SESConfig.attachmentParallelism = attachmentParallelism;
    }

    public static String getAwsAccessKey() {
        return awsAccessKey;
    }

    public static void setAwsAccessKey(String awsAccessKey) {
        SESConfig.awsAccessKey = awsAccessKey;
    }

    public static Region getAwsRegion() {
        return awsRegion;
    }

    public static void setAwsRegion(Region awsRegion) {
        SESConfig.awsRegion = awsRegion;
    }

    public static String getAwsSecretKey() {
        return awsSecretKey;
    }

    public static void setAwsSecretKey(String awsSecretKey) {
        SESConfig.awsSecretKey = awsSecretKey;
    }

    public static String getName() {
        return name;
    }

    public static void setName(String name) {
        SESConfig.name = name;
    }

    public static int getPrepBatchSize() {
        return prepBatchSize;
    }

    public static void setPrepBatchSize(int prepBatchSize) {
        SESConfig.prepBatchSize = prepBatchSize;
    }

    public static int getPrepParallelism() {
        return prepParallelism;
    }

    public static void setPrepParallelism(int prepParallelism) {
        SESConfig.prepParallelism = prepParallelism;
    }

    public static int getSendBatchSize() {
        return sendBatchSize;
    }

    public static void setSendBatchSize(int sendBatchSize) {
        SESConfig.sendBatchSize = sendBatchSize;
    }

    public static int getSendParallelism() {
        return sendParallelism;
    }

    public static void setSendParallelism(int sendParallelism) {
        SESConfig.sendParallelism = sendParallelism;
    }

    public static int getSendRetryMax() {
        return sendRetryMax;
    }

    public static void setSendRetryMax(int sendRetryMax) {
        SESConfig.sendRetryMax = sendRetryMax;
    }
}
