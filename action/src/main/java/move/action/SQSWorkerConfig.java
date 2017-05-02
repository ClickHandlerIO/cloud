package move.action;

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
    * Indicates FIFO queue rules to be used.
    */
   public boolean fifo = false;
   /**
    *
    */
   public boolean consumer = false;
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
   public boolean alwaysUseS3 = false;
   /**
    * The size in bytes that determines when to use S3 for message payload.
    * If "alwaysUseS3" is set to true, then this parameter is ignored.
    * If set to 0 then messages larger than 256kb will we stored in S3.
    * Value must be between 0 and 262000.
    */
   @JsonProperty
   public int s3MessageThreshold = 0;

   public String name() {
      return this.name;
   }

   public String sqsName() {
      return this.sqsName;
   }

   public boolean fifo() {
      return this.fifo;
   }

   public boolean consumer() {
      return this.consumer;
   }

   public String region() {
      return this.region;
   }

   public String accessKey() {
      return this.accessKey;
   }

   public String secretKey() {
      return this.secretKey;
   }

   public String s3Bucket() {
      return this.s3Bucket;
   }

   public String s3AccessKey() {
      return this.s3AccessKey;
   }

   public String s3SecretKey() {
      return this.s3SecretKey;
   }

   public boolean alwaysUseS3() {
      return this.alwaysUseS3;
   }

   public int s3MessageThreshold() {
      return this.s3MessageThreshold;
   }

   public SQSWorkerConfig name(final String name) {
      this.name = name;
      return this;
   }

   public SQSWorkerConfig sqsName(final String sqsName) {
      this.sqsName = sqsName;
      return this;
   }

   public SQSWorkerConfig fifo(final boolean fifo) {
      this.fifo = fifo;
      return this;
   }

   public SQSWorkerConfig consumer(final boolean consumer) {
      this.consumer = consumer;
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
