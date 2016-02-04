package s3.config;

/**
 * Configuration settings for S3 file service.
 *
 * @author Brad Behnke
 */
public class S3Config {
    private String awsAccessKey;
    private String awsSecretKey;
    private String endPoint = "s3-us-west-1.amazonaws.com";

    public String getAwsAccessKey() {
        return awsAccessKey;
    }

    public S3Config awsAccessKey(String awsAccessKey) {
        this.awsAccessKey = awsAccessKey;
        return this;
    }

    public String getAwsSecretKey() {
        return awsSecretKey;
    }

    public S3Config awsSecretKey(String awsSecretKey) {
        this.awsSecretKey = awsSecretKey;
        return this;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public S3Config endPoint(String endPoint) {
        this.endPoint = endPoint;
        return this;
    }
}
