package io.clickhandler.action;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.util.concurrent.AbstractIdleService;

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
    public static final String ATTRIBUTE_TYPE = "t";
    private final Map<String, QueueContext> queueMap = new HashMap<>();
    private AmazonSQSClient sqsClient;

    @Inject
    SQSService() {
    }

    public void setSqsClient(AmazonSQSClient sqsClient) {
        this.sqsClient = sqsClient;
    }

    @Override
    protected void startUp() throws Exception {
        if (sqsClient == null) {
            throw new RuntimeException("WorkerSQSClientProvider has no mapping.");
        }

        // Build Queue map.
        final Multimap<String, WorkerActionProvider> map = Multimaps.newListMultimap(new HashMap<>(), ArrayList::new);
        ActionManager.getWorkerActionMap().values().forEach(provider -> map.put(provider.getQueueName(), provider));

        final ActionManagerConfig actionManagerConfig = ActionManager.getConfig();
        List<WorkerConfig> workerConfigList = actionManagerConfig.workerConfigs;
        if (workerConfigList == null)
            workerConfigList = new ArrayList<>(0);
        final Map<String, WorkerConfig> workerConfigs = workerConfigList.stream()
            .collect(Collectors.toMap(k -> k.name, Function.identity()));

        map.asMap().entrySet().forEach(entry -> {
            WorkerConfig workerConfig = workerConfigs.get(entry.getKey());
            if (workerConfig == null) {
                workerConfig = new WorkerConfig().name(entry.getKey());
            }
            SQSWorkerConfig sqsWorkerConfig = workerConfig.sqsConfig;
            if (sqsWorkerConfig == null) {
                sqsWorkerConfig = new SQSWorkerConfig();
            }

            final GetQueueUrlResult result = sqsClient.getQueueUrl(entry.getKey());
            final String queueUrl = result.getQueueUrl();

            final SQSSender sender = new SQSSender();
            sender.setQueueUrl(queueUrl);
            sender.setSqsClient(sqsClient);

            final SQSReceiver receiver;
            if (actionManagerConfig.worker) {
                receiver = new SQSReceiver();
                receiver.setQueueUrl(queueUrl);
                receiver.setSqsClient(sqsClient);
                receiver.setConfig(sqsWorkerConfig);
            } else {
                receiver = null;
            }

            if (entry.getValue() != null) {
                for (WorkerActionProvider actionProvider : entry.getValue()) {
                    actionProvider.setSender(sender);
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
        final SQSSender sender;
        final SQSReceiver receiver;

        public QueueContext(SQSSender sender, SQSReceiver receiver) {
            this.sender = sender;
            this.receiver = receiver;
        }
    }
}
