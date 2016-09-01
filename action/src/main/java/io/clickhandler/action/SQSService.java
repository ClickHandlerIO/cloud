package io.clickhandler.action;

import com.amazon.sqs.javamessaging.AmazonSQSExtendedClient;
import com.amazon.sqs.javamessaging.ExtendedClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.http.AmazonHttpClient;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Iterators;
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
import java.util.*;

/**
 *
 */
@Singleton
public class SQSService extends AbstractIdleService implements WorkerService {
    static final String ATTRIBUTE_NAME = "n";
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

        // Build Queue map.
        final Multimap<String, WorkerActionProvider> map = Multimaps.newSetMultimap(
            new HashMap<>(), HashSet::new
        );
        ActionManager.getWorkerActionMap().values().forEach(
            provider -> map.put(provider.getQueueName(), provider)
        );
        if (map.isEmpty()) {
            LOG.warn("No WorkerActions were registered.");
            return;
        }

        // Get worker configs.
        final List<SQSWorkerConfig> workerConfigs = config.workers == null ? new ArrayList<>() : config.workers;

        // Create SQS Clients.
        workerConfigs.forEach(workerConfig -> {
            final Collection<WorkerActionProvider> actionProviders = map.get(workerConfig.name);
            if (actionProviders == null || actionProviders.isEmpty()) {
                LOG.warn("No WorkerActions are mapped to Queue [" + workerConfig.name + "]");
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

            final AmazonS3Client s3Client;
            final String s3Bucket = Strings.nullToEmpty(workerConfig.s3Bucket).trim();
            final String s3AwsAccessKey = Strings.nullToEmpty(workerConfig.s3AccessKey).trim();
            final String s3AwsSecretKey = Strings.nullToEmpty(workerConfig.s3SecretKey).trim();

            if (s3Bucket.isEmpty()) {
                s3Client = null;
            } else if (s3AwsAccessKey.isEmpty()) {
                s3Client = new AmazonS3Client();
            } else {
                s3Client = new AmazonS3Client(new BasicAWSCredentials(s3AwsAccessKey, s3AwsSecretKey));
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
            final AmazonSQS sqsDeleteClient;
            final AmazonSQS sqsReceiveClient;

            if (awsAccessKey.isEmpty()) {
                sqsSendClient = s3Client == null ?
                    new AmazonSQSClient() :
                    new AmazonSQSExtendedClient(new AmazonSQSClient(), extendedClientConfiguration);

                if (workerConfig.receiveThreads > 0) {
                    sqsReceiveClient = s3Client == null ?
                        new AmazonSQSClient() :
                        new AmazonSQSExtendedClient(new AmazonSQSClient(), extendedClientConfiguration);

                    sqsDeleteClient = s3Client == null ?
                        new AmazonSQSClient() :
                        new AmazonSQSExtendedClient(new AmazonSQSClient(), extendedClientConfiguration);
                } else {
                    sqsReceiveClient = null;
                    sqsDeleteClient = null;
                }
            } else {
                sqsSendClient = s3Client == null ?
                    new AmazonSQSClient(new BasicAWSCredentials(awsAccessKey, awsSecretKey)) :
                    new AmazonSQSExtendedClient(
                        new AmazonSQSClient(
                            new BasicAWSCredentials(awsAccessKey, awsSecretKey)), extendedClientConfiguration);

                if (workerConfig.receiveThreads > 0) {
                    sqsReceiveClient = s3Client == null ?
                        new AmazonSQSClient(new BasicAWSCredentials(awsAccessKey, awsSecretKey)) :
                        new AmazonSQSExtendedClient(
                            new AmazonSQSClient(
                                new BasicAWSCredentials(awsAccessKey, awsSecretKey)), extendedClientConfiguration);

                    sqsDeleteClient = s3Client == null ?
                        new AmazonSQSClient(new BasicAWSCredentials(awsAccessKey, awsSecretKey)) :
                        new AmazonSQSExtendedClient(
                            new AmazonSQSClient(
                                new BasicAWSCredentials(awsAccessKey, awsSecretKey)), extendedClientConfiguration);
                } else {
                    sqsReceiveClient = null;
                    sqsDeleteClient = null;
                }
            }

            // Set region.
            sqsSendClient.setRegion(Region.getRegion(region));
            if (s3Client != null) {
                s3Client.setRegion(Region.getRegion(region));
            }
            if (sqsReceiveClient != null) {
                sqsReceiveClient.setRegion(Region.getRegion(region));
            }
            if (sqsDeleteClient != null) {
                sqsDeleteClient.setRegion(Region.getRegion(region));
            }

            WorkerActionProvider dedicated = null;
            if (actionProviders.size() > 1) {
                actionProviders.forEach($ ->
                    Preconditions.checkArgument(
                        !$.getWorkerAction().dedicated(),
                        "Queue [" +
                            workerConfig.name +
                            "] with dedicated WorkerAction [" +
                            $.getActionClass().getCanonicalName() +
                            "] cannot have other WorkerActions mapped to it."
                    )
                );
            } else if (actionProviders.size() == 1) {
                dedicated = Iterators.get(actionProviders.iterator(), 0);
                if (!dedicated.getWorkerAction().dedicated())
                    dedicated = null;
            } else {
                LOG.warn("Queue [" + workerConfig.name + "] is not mapped to any WorkerActions.");
                return;
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
            if (workerConfig.receiveThreads > 0) {
                // Create a SQSConsumer to receive Worker Requests.
                consumer = new SQSConsumer();
                consumer.setQueueUrl(queueUrl);
                consumer.setSqsReceiveClient(sqsReceiveClient);
                consumer.setSqsDeleteClient(sqsDeleteClient);
                consumer.setConfig(workerConfig);

                // Set dedicated if necessary.
                if (dedicated != null && dedicated.getWorkerAction().dedicated()) {
                    consumer.setDedicated(dedicated);
                }
            } else {
                consumer = null;
            }

            for (WorkerActionProvider actionProvider : actionProviders) {
                actionProvider.setProducer(producer);
            }

            queueMap.put(workerConfig.name, new QueueContext(producer, consumer, sqsSendClient));
        });

        queueMap.values().forEach(queueContext -> {
            queueContext.sender.startAsync().awaitRunning();
            if (queueContext.receiver != null) {
                queueContext.receiver.startAsync().awaitRunning();
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

        public QueueContext(SQSProducer sender, SQSConsumer receiver, AmazonSQS sqsClient) {
            this.sender = sender;
            this.receiver = receiver;
            this.sqsClient = sqsClient;
        }
    }
}
