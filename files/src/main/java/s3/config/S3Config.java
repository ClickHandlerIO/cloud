package s3.config;

/**
 * Configuration settings for S3 file service.
 *
 * @author Brad Behnke
 */
public class S3Config {
    private static String name = "SES";
    private static String awsAccessKey;
    private static String awsSecretKey;
    private static String endPoint = "s3-us-west-1.amazonaws.com";
    private static int threads = 10;

    public static String getAwsAccessKey() {
        return awsAccessKey;
    }

    public static void setAwsAccessKey(String awsAccessKey) {
        S3Config.awsAccessKey = awsAccessKey;
    }

    public static String getAwsSecretKey() {
        return awsSecretKey;
    }

    public static void setAwsSecretKey(String awsSecretKey) {
        S3Config.awsSecretKey = awsSecretKey;
    }

    public static String getEndPoint() {
        return endPoint;
    }

    public static void setEndPoint(String endPoint) {
        S3Config.endPoint = endPoint;
    }

    public static String getName() {
        return name;
    }

    public static void setName(String name) {
        S3Config.name = name;
    }

    public static int getThreads() {
        return threads;
    }

    public static void setThreads(int threads) {
        S3Config.threads = threads;
    }
}
