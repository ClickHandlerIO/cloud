package io.clickhandler.action;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.google.common.base.Preconditions;
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
    static final String ATTRIBUTE_TYPE = "t";
    private static final Logger LOG = LoggerFactory.getLogger(SQSService.class);

    private final Map<String, QueueContext> queueMap = new HashMap<>();
    @Inject
    Vertx vertx;
    private AmazonSQSClient sqsClient;

    @Inject
    SQSService() {
    }

    void setSqsClient(AmazonSQSClient sqsClient) {
        this.sqsClient = sqsClient;
    }

    @Override
    protected void startUp() throws Exception {
        Preconditions.checkNotNull(sqsClient, "AmazonSQSClient must be set.");

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
        final ActionManagerConfig actionManagerConfig = ActionManager.getConfig();
        List<WorkerConfig> workerConfigList = actionManagerConfig.workerConfigs;
        if (workerConfigList == null)
            workerConfigList = new ArrayList<>(0);
        final Map<String, WorkerConfig> workerConfigs = workerConfigList.stream()
            .collect(Collectors.toMap(k -> k.name, Function.identity()));

        map.asMap().entrySet().forEach(entry -> {
            WorkerConfig workerConfig = workerConfigs.get(entry.getKey());
            if (workerConfig == null) {
                LOG.warn("WorkerConfig for Queue '" + entry.getKey() + "' was not found. Creating a default one.");
                workerConfig = new WorkerConfig().name(entry.getKey());
            }
            SQSWorkerConfig sqsWorkerConfig = workerConfig.sqsConfig;
            if (sqsWorkerConfig == null) {
                LOG.warn("SQSWorkerConfig for Queue '" + entry.getKey() + "' was not found. Creating a default one.");
                workerConfig.sqsConfig = sqsWorkerConfig = new SQSWorkerConfig();
            }

            // Get QueueURL from AWS.
            final GetQueueUrlResult result = sqsClient.getQueueUrl(entry.getKey());
            final String queueUrl = result.getQueueUrl();

            // Create producer.
            final SQSProducer sender = new SQSProducer(vertx);
            sender.setQueueUrl(queueUrl);
            sender.setSqsClient(sqsClient);
            sender.setConfig(sqsWorkerConfig);

            final SQSConsumer receiver;
            // Is this node configured to be a "Worker"?
            if (actionManagerConfig.worker) {
                // Create a SQSConsumer to receive Worker Requests.
                receiver = new SQSConsumer();
                receiver.setQueueUrl(queueUrl);
                receiver.setSqsClient(sqsClient);
                receiver.setConfig(sqsWorkerConfig);
            } else {
                receiver = null;
            }

            if (entry.getValue() != null) {
                for (WorkerActionProvider actionProvider : entry.getValue()) {
                    actionProvider.setProducer(sender);
                }
            }

            queueMap.put(entry.getKey(), new QueueContext(sender, receiver));
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

    private final class QueueContext {
        final SQSProducer sender;
        final SQSConsumer receiver;

        public QueueContext(SQSProducer sender, SQSConsumer receiver) {
            this.sender = sender;
            this.receiver = receiver;
        }
    }
}
