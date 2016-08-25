package io.clickhandler.action;

import com.amazon.sqs.javamessaging.AmazonSQSExtendedClient;
import com.amazon.sqs.javamessaging.ExtendedClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.util.concurrent.AbstractIdleService;
import io.vertx.rxjava.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 */
@Singleton
public class SQSService extends AbstractIdleService implements WorkerService {
    static final String ATTRIBUTE_NAME = "n";
    private static final Logger LOG = LoggerFactory.getLogger(SQSService.class);

    private final Map<String, QueueContext> queueMap = new HashMap<>();
    private final Map<String, AmazonSQSExtendedClient> sqsClientMap = new HashMap<>();
    private final Map<String, AmazonS3> s3ClientMap = new HashMap<>();
    @Inject
    Vertx vertx;
    private SQSConfig config;

    @Inject
    SQSService() {
    }

    public static void main(String[] args) {
        Preconditions.checkArgument(true, "True");
        Preconditions.checkArgument(!false, "False");
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

        // Build Queue map.
        final Multimap<String, WorkerActionProvider> map = Multimaps.newListMultimap(
            new HashMap<>(), ArrayList::new
        );
        ActionManager.getWorkerActionMap().values().forEach(
            provider -> map.put(provider.getQueueName(), provider)
        );
        if (map.isEmpty()) {
            LOG.warn("No WorkerActions were registered.");
            return;
        }

        // Get worker configs.
        List<SQSWorkerConfig> workerConfigList = config.workers;
        if (workerConfigList == null)
            workerConfigList = new ArrayList<>(0);
        final Map<String, SQSWorkerConfig> workerConfigs = workerConfigList.stream()
            .collect(Collectors.toMap(k -> k.name, Function.identity()));

        // Create SQS Clients.
        map.asMap().entrySet().forEach(entry -> {
                SQSWorkerConfig workerConfig = workerConfigs.get(entry.getKey());
                if (workerConfig == null) {
                    LOG.warn("SQSWorkerConfig for Queue '" + entry.getKey() + "' was not found.");
                    return;
                }
                final String regionName = Strings.nullToEmpty(workerConfig.region).trim();
                Preconditions.checkArgument(
                    regionName.isEmpty(),
                    "SQSWorkerConfig for Queue '" + entry.getKey() + "' does not have a region specified"
                );

                AmazonSQSExtendedClient client = sqsClientMap.get(regionName);
                if (client != null)
                    return;

                final Regions region = Regions.fromName(regionName);
                Preconditions.checkNotNull(region,
                    "SQSWorkerConfig for Queue '" +
                        entry.getKey() +
                        "' region '" +
                        regionName +
                        "' is not a valid AWS region name."
                );

                final AmazonS3Client s3Client;
                final String s3AwsAccessKey = Strings.nullToEmpty(workerConfig.s3AccessKey).trim();
                final String s3AwsSecretKey = Strings.nullToEmpty(workerConfig.s3SecretKey).trim();

                if (s3AwsAccessKey.isEmpty()) {
                    s3Client = new AmazonS3Client();
                } else {
                    s3Client = new AmazonS3Client(new BasicAWSCredentials(s3AwsAccessKey, s3AwsSecretKey));
                }

                // Put in s3ClientMap.
                s3ClientMap.put(regionName, s3Client);

                final String awsAccessKey = Strings.nullToEmpty(workerConfig.accessKey).trim();
                final String awsSecretKey = Strings.nullToEmpty(workerConfig.secretKey).trim();

                final ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration();
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

                // Create SQSClient.
                final AmazonSQSClient sqsClient;
                if (awsAccessKey.isEmpty()) {
                    sqsClient = new AmazonSQSClient();
                } else {
                    sqsClient = new AmazonSQSClient(new BasicAWSCredentials(awsAccessKey, awsSecretKey));
                }

                // Set region.
                sqsClient.setRegion(Region.getRegion(region));
                s3Client.setRegion(Region.getRegion(region));

                // Put in sqsClientMap.
                sqsClientMap.put(regionName, new AmazonSQSExtendedClient(sqsClient, extendedClientConfiguration));
            }
        );

        map.asMap().entrySet().forEach(entry -> {
            final SQSWorkerConfig workerConfig = workerConfigs.get(entry.getKey());
            Preconditions.checkNotNull(
                workerConfig,
                "SQSWorkerConfig for Queue '" + entry.getKey() + "' was not found."
            );

            final AmazonSQSExtendedClient sqsClient = sqsClientMap.get(workerConfig.region);
            Preconditions.checkNotNull(
                sqsClient,
                "AmazonSQSClient was not found for region '" + workerConfig.region + "'"
            );

            WorkerActionProvider dedicated = null;
            if (entry.getValue().size() > 1) {
                entry.getValue().forEach($ ->
                    Preconditions.checkArgument(
                        !$.getWorkerAction().dedicated(),
                        "Queue [" +
                            entry.getKey() +
                            "] with dedicated WorkerAction [" +
                            $.getActionClass().getCanonicalName() +
                            "] cannot have other WorkerActions mapped to it."
                    )
                );
            } else if (entry.getValue().size() == 1) {
                dedicated = Iterators.get(entry.getValue().iterator(), 0);
            } else {
                LOG.warn("Queue [" + entry.getKey() + "] is not mapped to any WorkerActions.");
                return;
            }

            // Get QueueURL from AWS.
            final GetQueueUrlResult result = sqsClient.getQueueUrl(entry.getKey());
            final String queueUrl = result.getQueueUrl();

            // Create producer.
            final SQSProducer producer = new SQSProducer(vertx);
            producer.setQueueUrl(queueUrl);
            producer.setSqsClient(sqsClient);
            producer.setConfig(workerConfig);

            // Create a SQSConsumer to receive Worker Requests.
            final SQSConsumer consumer = new SQSConsumer();
            consumer.setQueueUrl(queueUrl);
            consumer.setSqsClient(sqsClient);
            consumer.setConfig(workerConfig);

            // Set dedicated if necessary.
            if (dedicated != null && dedicated.getWorkerAction().dedicated()) {
                consumer.setDedicated(dedicated);
            }

            if (entry.getValue() != null) {
                for (WorkerActionProvider actionProvider : entry.getValue()) {
                    actionProvider.setProducer(producer);
                }
            }

            queueMap.put(entry.getKey(), new QueueContext(producer, consumer));
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

        public QueueContext(SQSProducer sender, SQSConsumer receiver) {
            this.sender = sender;
            this.receiver = receiver;
        }
    }
}
