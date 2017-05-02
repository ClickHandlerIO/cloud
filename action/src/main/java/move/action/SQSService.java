package move.action;

import com.amazon.sqs.javamessaging.AmazonSQSExtendedClient;
import com.amazon.sqs.javamessaging.ExtendedClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.http.AmazonHttpClient;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.util.concurrent.AbstractIdleService;
import io.vertx.rxjava.core.Vertx;
import javaslang.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Manages the lifecycle of all SQS Producers and Consumers.
 *
 * @author Clay Molocznik
 */
@Singleton
public class SQSService extends AbstractIdleService implements WorkerService {
    private static final Logger LOG = LoggerFactory.getLogger(SQSService.class);

    private final Multimap<String, QueueContext> queueMap =
        Multimaps.newListMultimap(new HashMap<>(), ArrayList::new);

    @Inject
    Vertx vertx;
    private SQSConfig config;

    @Inject
    SQSService() {
    }

    /**
     * @param config
     */
    public void setConfig(SQSConfig config) {
        this.config = config;
    }

    @Override
    protected void startUp() throws Exception {
        Preconditions.checkNotNull(config, "config must be set.");

        final Logger amazonClientLogger = LoggerFactory.getLogger(AmazonHttpClient.class);
        Try.run(() -> {
            final Class param = Class.forName("ch.qos.logback.classic.Level");
            Field errorField = param.getDeclaredField("ERROR");
            Method method = amazonClientLogger.getClass().getMethod("setLevel", param);
            Object value = errorField.get(param);
            if (method != null) {
                method.invoke(amazonClientLogger, value);
            }
        });

        // Get worker configs.
        final List<SQSWorkerConfig> workerConfigs = config.workers == null ? new ArrayList<>() : config.workers;

        // Create SQS Clients.
        workerConfigs.forEach(workerConfig -> {
            final WorkerActionProvider actionProvider = ActionManager.getWorkerActionMap().get(workerConfig.name);
            if (actionProvider == null) {
                LOG.warn("Worker Action '" + workerConfig.name + " does not exist.");
                return;
            }

            final String regionName = Strings.nullToEmpty(workerConfig.region).trim();
            Preconditions.checkArgument(
                !regionName.isEmpty(),
                "SQSWorkerConfig for Queue '" + workerConfig.name + "' does not have a region specified"
            );

            final Regions region = Regions.fromName(regionName);
            Preconditions.checkNotNull(region,
                "SQSWorkerConfig for Queue '" +
                    workerConfig.name +
                    "' region '" +
                    regionName +
                    "' is not a valid AWS region name.");

            final AmazonS3 s3Client;
            final String s3Bucket = Strings.nullToEmpty(workerConfig.s3Bucket).trim();
            final String s3AwsAccessKey = Strings.nullToEmpty(workerConfig.s3AccessKey).trim();
            final String s3AwsSecretKey = Strings.nullToEmpty(workerConfig.s3SecretKey).trim();

            if (s3Bucket.isEmpty()) {
                s3Client = null;
            } else if (s3AwsAccessKey.isEmpty()) {
                s3Client = AmazonS3Client.builder()
                    .withRegion(region)
                    .build();
            } else {
                s3Client = AmazonS3Client.builder()
                    .withRegion(region)
                    .withCredentials(
                        new AWSStaticCredentialsProvider(
                            new BasicAWSCredentials(s3AwsAccessKey, s3AwsSecretKey)))
                    .build();
            }

            final String awsAccessKey = Strings.nullToEmpty(workerConfig.accessKey).trim();
            final String awsSecretKey = Strings.nullToEmpty(workerConfig.secretKey).trim();

            ExtendedClientConfiguration extendedClientConfiguration = null;

            if (s3Client != null) {
                extendedClientConfiguration = new ExtendedClientConfiguration();
                if (workerConfig.alwaysUseS3) {
                    extendedClientConfiguration.withAlwaysThroughS3(true);
                } else if (workerConfig.s3MessageThreshold > 0) {
                    extendedClientConfiguration.withMessageSizeThreshold(workerConfig.s3MessageThreshold);
                }

                // Set bucket.
                extendedClientConfiguration.withLargePayloadSupportEnabled(
                    s3Client,
                    workerConfig.s3Bucket
                );
            }

            // Create SQSClient.
            final AmazonSQS sqsSendClient;
            final AmazonSQS sqsReceiveClient;

            if (awsAccessKey.isEmpty()) {
                sqsSendClient = s3Client == null ?
                    AmazonSQSClient.builder().withRegion(region).build() :
                    new AmazonSQSExtendedClient(
                        AmazonSQSClient.builder().withRegion(region).build(),
                        extendedClientConfiguration
                    );

                if (workerConfig.consumer) {
                    sqsReceiveClient = s3Client == null ?
                        AmazonSQSClient.builder().withRegion(region).build() :
                        new AmazonSQSExtendedClient(
                            AmazonSQSClient.builder().withRegion(region).build(),
                            extendedClientConfiguration
                        );
                } else {
                    sqsReceiveClient = null;
                }
            } else {
                sqsSendClient = s3Client == null ?
                    AmazonSQSClient.builder()
                        .withRegion(region)
                        .withCredentials(
                            new AWSStaticCredentialsProvider(
                                new BasicAWSCredentials(awsAccessKey, awsSecretKey)))
                        .build() :
                    new AmazonSQSExtendedClient(
                        AmazonSQSClient.builder()
                            .withRegion(region)
                            .withCredentials(
                                new AWSStaticCredentialsProvider(
                                    new BasicAWSCredentials(awsAccessKey, awsSecretKey)))
                            .build(),
                        extendedClientConfiguration
                    );

                if (workerConfig.consumer) {
                    sqsReceiveClient = s3Client == null ?
                        AmazonSQSClient.builder()
                            .withRegion(region)
                            .withCredentials(
                                new AWSStaticCredentialsProvider(
                                    new BasicAWSCredentials(awsAccessKey, awsSecretKey)))
                            .build() :
                        new AmazonSQSExtendedClient(
                            AmazonSQSClient.builder()
                                .withRegion(region)
                                .withCredentials(
                                    new AWSStaticCredentialsProvider(
                                        new BasicAWSCredentials(awsAccessKey, awsSecretKey)))
                                .build(), extendedClientConfiguration);
                } else {
                    sqsReceiveClient = null;
                }
            }

            // Get QueueURL from AWS.
            final GetQueueUrlResult result = sqsSendClient.getQueueUrl(workerConfig.sqsName);
            final String queueUrl = result.getQueueUrl();

            // Create producer.
            final SQSProducer producer = new SQSProducer(vertx);
            producer.setQueueUrl(queueUrl);
            producer.setSqsClient(sqsSendClient);
            producer.setConfig(workerConfig);

            final SQSConsumer consumer;
            if (workerConfig.consumer) {
                // Create a SQSConsumer to receive Worker Requests.
                consumer = new SQSConsumer(vertx);
                consumer.setQueueUrl(queueUrl);
                consumer.setSqlClient(sqsReceiveClient);
                consumer.setActionProvider(actionProvider);
                consumer.setConfig(workerConfig);
            } else {
                consumer = null;
            }

            actionProvider.setProducer(producer);

            queueMap.put(workerConfig.name, new QueueContext(producer, consumer, sqsSendClient, workerConfig));
        });

        queueMap.values().forEach(queueContext -> {
            try {
                queueContext.sender.startAsync().awaitRunning();
            } catch (Throwable e) {
                LOG.error("Failed to start SQSProducer for '" + queueContext.config.name + "'");
                throw new RuntimeException(e);
            }
            if (queueContext.receiver != null) {
                try {
                    queueContext.receiver.startAsync().awaitRunning();
                } catch (Throwable e) {
                    LOG.error("Failed to start SQSConsumer for '" + queueContext.config.name + "'");
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Override
    protected void shutDown() throws Exception {
        queueMap.values().forEach(queueContext -> {
            queueContext.sender.stopAsync().awaitTerminated();
            if (queueContext.receiver != null) {
                queueContext.receiver.stopAsync().awaitTerminated();
            }
        });
    }

    /**
     *
     */
    private final class QueueContext {
        final SQSProducer sender;
        final SQSConsumer receiver;
        final AmazonSQS sqsClient;
        final SQSWorkerConfig config;

        public QueueContext(SQSProducer sender, SQSConsumer receiver, AmazonSQS sqsClient, SQSWorkerConfig config) {
            this.sender = sender;
            this.receiver = receiver;
            this.sqsClient = sqsClient;
            this.config = config;
        }
    }
}
