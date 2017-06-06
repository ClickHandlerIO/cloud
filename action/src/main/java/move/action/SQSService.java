package move.action;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.http.AmazonHttpClient;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.buffered.MoveAmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.buffered.QueueBuffer;
import com.amazonaws.services.sqs.buffered.QueueBufferConfig;
import com.amazonaws.services.sqs.model.*;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.AbstractIdleService;
import com.netflix.hystrix.exception.HystrixTimeoutException;
import io.vertx.rxjava.core.Vertx;
import javaslang.control.Try;
import move.common.Metrics;
import move.common.UID;
import move.common.WireFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Single;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Manages the lifecycle of all SQS Producers and Consumers.
 *
 * @author Clay Molocznik
 */
@Singleton
public class SQSService extends AbstractIdleService implements WorkerService {
    private static final Logger LOG = LoggerFactory.getLogger(SQSService.class);

    private static final int MIN_THREADS = 1;
    private static final int MAX_THREADS = 200;
    private static final long KEEP_ALIVE_SECONDS = 60L;

    private final Map<Class, QueueContext> queueMap = new HashMap<>();

    @Inject
    Vertx vertx;

    private SQSConfig config;
    private AmazonSQSAsync realSQS;
    private MoveAmazonSQSBufferedAsyncClient client;
    private ExecutorService bufferExecutor;

    @Inject
    SQSService() {
    }

    /**
     * @param config
     */
    public void setConfig(SQSConfig config) {
        this.config = config;
    }

    private boolean workerEnabled(WorkerActionProvider provider) {
        if (!config.worker) {
            return false;
        }

        if (config.exclusions != null && !config.exclusions.isEmpty()) {
            return !config.exclusions.contains(provider.name)
                && !config.exclusions.contains(provider.queueName);
        }

        if (config.inclusions != null && !config.inclusions.isEmpty()) {
            return config.inclusions.contains(provider.name)
                || config.inclusions.contains(provider.queueName);
        }

        return true;
    }

    private SQSQueueConfig queueConfig(WorkerActionProvider provider) {
        SQSQueueConfig queueConfig = queueConfig0(provider);

        if (queueConfig == null) {
            queueConfig = new SQSQueueConfig();
        }

        if (queueConfig.maxBatchSize < 1) {
            queueConfig.maxBatchSize = config.maxBatchSize;
        }

        if (queueConfig.maxBatchOpenMs < 0) {
            queueConfig.maxBatchOpenMs = config.maxBatchOpenMs;
        }

        if (queueConfig.maxInflightReceiveBatches < 0) {
            queueConfig.maxInflightReceiveBatches = config.maxInflightReceiveBatches;
        }

        if (queueConfig.maxDoneReceiveBatches < 0) {
            queueConfig.maxDoneReceiveBatches = config.maxDoneReceiveBatches;
        }

        return queueConfig;
    }

    private SQSQueueConfig queueConfig0(WorkerActionProvider provider) {
        if (config.queues == null || config.queues.isEmpty()) {
            return null;
        }

        return config.queues.stream()
            .filter($ -> Strings.nullToEmpty($.name).equalsIgnoreCase(provider.name))
            .findFirst()
            .orElse(null);
    }

    @Override
    protected void startUp() throws Exception {
        Preconditions.checkNotNull(config, "config must be set.");

        // AmazonHttpClient is vary chatty with log output. Shut it up.
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

        config.clientThreads = config.clientThreads < MIN_THREADS
                               ? MIN_THREADS
                               : config.clientThreads > MAX_THREADS
                                 ? MAX_THREADS
                                 : config.clientThreads;

        config.bufferThreads = config.bufferThreads < MIN_THREADS
                               ? MIN_THREADS
                               : config.bufferThreads > MAX_THREADS
                                 ? MAX_THREADS
                                 : config.bufferThreads;

        bufferExecutor = new ThreadPoolExecutor(
            0,
            config.bufferThreads,
            KEEP_ALIVE_SECONDS,
            TimeUnit.SECONDS,
            new SynchronousQueue<>()
        );

        final AmazonSQSAsyncClientBuilder builder = AmazonSQSAsyncClientBuilder
            .standard()
            .withExecutorFactory(() -> new ThreadPoolExecutor(
                0,
                config.clientThreads,
                KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new SynchronousQueue<>()
            ))
            .withRegion(config.region);

        config.awsAccessKey = Strings.nullToEmpty(config.awsAccessKey).trim();
        config.awsSecretKey = Strings.nullToEmpty(config.awsSecretKey).trim();

        // Set credentials if necessary.
        if (!config.awsAccessKey.isEmpty()) {
            builder.withCredentials(
                new AWSStaticCredentialsProvider(
                    new BasicAWSCredentials(
                        config.awsAccessKey, config.awsSecretKey)));
        }

        builder.withClientConfiguration(new ClientConfiguration()
            // Give a nice buffer to max connections based on max threads.
            .withMaxConnections(config.clientThreads * 2)
        );

        realSQS = builder.build();
        client = new MoveAmazonSQSBufferedAsyncClient(realSQS);

        ActionManager.workerActionMap().forEach((key, provider) -> {
            LOG.info("Calling getQueueUrl() for " + provider.queueName);

            final GetQueueUrlResult result = realSQS.getQueueUrl(provider.queueName);

            if (result == null || result.getQueueUrl() == null || result.getQueueUrl().isEmpty()) {
                final CreateQueueResult createQueueResult = realSQS.createQueue(provider.queueName);

                provider.queueUrl = createQueueResult.getQueueUrl();
            }
            else {
                provider.queueUrl = result.getQueueUrl();
            }

            final SQSQueueConfig queueConfig = queueConfig(provider);

            final QueueBufferConfig bufferConfig = new QueueBufferConfig()
                .withMaxBatchSize(queueConfig.maxBatchSize)
                .withMaxBatchOpenMs(queueConfig.maxBatchOpenMs)
                .withFlushOnShutdown(config.flushOnShutdown)

                // Turn off pre-fetching
                .withMaxInflightReceiveBatches(0)
                .withMaxDoneReceiveBatches(0);

            final boolean isWorker = workerEnabled(provider);
            if (isWorker) {
                // Enable pre-fetching
                bufferConfig.withMaxInflightReceiveBatches(queueConfig.maxInflightReceiveBatches);
                bufferConfig.withMaxDoneReceiveBatches(queueConfig.maxDoneReceiveBatches);

                if (queueConfig.maxInflightReceiveBatches > 0 && queueConfig.maxDoneReceiveBatches > 0) {
                    bufferConfig.withVisibilityTimeoutSeconds(
                        (int) TimeUnit.MILLISECONDS.toSeconds((long) (provider.getTimeoutMillis() * 2.1))
                    );
                }
                else {
                    bufferConfig.withVisibilityTimeoutSeconds(
                        (int) TimeUnit.MILLISECONDS.toSeconds((long) (provider.getTimeoutMillis() + 2000))
                    );
                }
            }

            final QueueBuffer buffer = client.putQBuffer(provider.queueUrl, bufferConfig, bufferExecutor);

            final Sender sender = new Sender(
                provider,
                provider.queueUrl,
                buffer
            );

            provider.setProducer(sender);

            Receiver consumer = null;
            if (isWorker) {
                consumer = new Receiver(vertx, provider, buffer, queueConfig.threads > 0
                                                                 ? queueConfig.threads
                                                                 : 0);
            }

            queueMap.put(provider.getActionClass(), new QueueContext(sender, consumer, queueConfig));
        });

        // Startup all receivers.
        queueMap.values().forEach(queueContext -> {
            if (queueContext.receiver != null) {
                try {
                    queueContext.receiver.startAsync().awaitRunning();
                }
                catch (Throwable e) {
                    LOG.error("Failed to start Receiver for '" + queueContext.config.name + "'");
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Override
    protected void shutDown() throws Exception {
        queueMap.values().forEach(queueContext -> {
            if (queueContext.receiver != null) {
                queueContext.receiver.stopAsync().awaitTerminated();
            }
        });

        client.shutdown();
    }

    private static class Sender implements WorkerProducer {
        private final WorkerActionProvider actionProvider;
        private final String queueUrl;
        private final QueueBuffer buffer;

        public Sender(WorkerActionProvider actionProvider, String queueUrl, QueueBuffer buffer) {
            this.actionProvider = actionProvider;
            this.queueUrl = queueUrl;
            this.buffer = buffer;
        }

        @Override
        public Single<Boolean> send(WorkerRequest request) {
            return Single.create(subscriber -> {
                final SendMessageRequest sendRequest = new SendMessageRequest(
                    queueUrl,
                    WireFormat.stringify(request.request)
                );

                if (request.delaySeconds > 0) {
                    sendRequest.setDelaySeconds(request.delaySeconds);
                }

                if (actionProvider.fifo) {
                    String groupId = actionProvider.name;

                    if (request.groupId != null && !request.groupId.isEmpty()) {
                        groupId = request.groupId;
                    }

                    sendRequest.setMessageGroupId(groupId);
                    sendRequest.setMessageDeduplicationId(UID.next());
                }

                buffer.sendMessage(sendRequest, new AsyncHandler<SendMessageRequest, SendMessageResult>() {
                    @Override
                    public void onError(Exception exception) {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onError(exception);
                        }
                    }

                    @Override
                    public void onSuccess(SendMessageRequest request, SendMessageResult sendMessageResult) {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onSuccess(sendMessageResult != null && sendMessageResult.getMessageId() != null && !sendMessageResult.getMessageId().isEmpty());
                        }
                    }
                });
            });
        }
    }

    /**
     * Processes Worker requests from a single AmazonSQS Queue.
     * Supports parallel receive and delete threads and utilizes SQS batching to increase message throughput.
     * <p>
     * Reliable
     */
    private static class Receiver extends AbstractIdleService {
        private static final Logger LOG = LoggerFactory.getLogger(Receiver.class);
        private static final int MIN_RECEIVE_THREADS = 1;
        private static final int MAX_RECEIVE_THREADS = 1024;

        private final Vertx vertx;
        private final QueueBuffer sqsBuffer;
        private final WorkerActionProvider actionProvider;
        private final String queueUrl;
        private int receiveThreadCount;
        private ReceiveThread[] receiveThreads;

        private Gauge<Long> secondsSinceLastPollGauge;
        private Counter receiveThreadsCounter;
        private Counter jobsCounter;
        private Counter timeoutsCounter;
        private Counter completesCounter;
        private Counter inCompletesCounter;
        private Counter exceptionsCounter;
        private Counter deletesCounter;
        private Counter deleteFailuresCounter;

        private volatile long lastPoll;

        public Receiver(final Vertx vertx,
                        final WorkerActionProvider actionProvider,
                        final QueueBuffer queueBuffer,
                        final int receiveThreadCount) {
            this.vertx = vertx;
            this.actionProvider = Preconditions.checkNotNull(actionProvider, "actionProvider must be specified");
            this.queueUrl = Preconditions.checkNotNull(actionProvider.queueUrl, "queueUrl must be specified");
            this.sqsBuffer = Preconditions.checkNotNull(queueBuffer, "queueBuffer must be specified");
            this.receiveThreadCount = receiveThreadCount;
        }

        @Override
        protected void startUp() throws Exception {
            LOG.debug("Starting up SQS service.");

            // Find ActionProvider.
            Preconditions.checkNotNull(actionProvider, "ActionProvider for '" + actionProvider.name + "' was not set.");

            final MetricRegistry registry = Metrics.registry();
            secondsSinceLastPollGauge = registry.register(
                actionProvider.name + "-SECONDS_SINCE_LAST_POLL",
                () -> TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastPoll)
            );
            receiveThreadsCounter = registry.counter(actionProvider.name + "-RECEIVE_THREADS");
            jobsCounter = registry.counter(actionProvider.name + "-JOBS");
            completesCounter = registry.counter(actionProvider.name + "-COMPLETES");
            inCompletesCounter = registry.counter(actionProvider.name + "-IN_COMPLETES");
            exceptionsCounter = registry.counter(actionProvider.name + "-EXCEPTIONS");
            timeoutsCounter = registry.counter(actionProvider.name + "-TIMEOUTS");
            deletesCounter = registry.counter(actionProvider.name + "-DELETES");
            deleteFailuresCounter = registry.counter(actionProvider.name + "-DELETE_FAILURES");

            int threads = actionProvider.getMaxConcurrentRequests();

            if (receiveThreadCount > 0) {
                threads = receiveThreadCount;
            }

            if (threads < MIN_RECEIVE_THREADS) {
                threads = MIN_RECEIVE_THREADS;
            }
            else if (threads > MAX_RECEIVE_THREADS) {
                threads = MAX_RECEIVE_THREADS;
            }

            // Start Receive threads.
            receiveThreads = new ReceiveThread[threads];
            for (int i = 0; i < receiveThreads.length; i++) {
                receiveThreads[i] = new ReceiveThread(i);
                receiveThreads[i].startAsync().awaitRunning();
            }
        }

        @Override
        protected void shutDown() throws Exception {
            sqsBuffer.shutdown();

            // Stop receiving messages.
            for (ReceiveThread thread : receiveThreads) {
                Try.run(() -> thread.stopAsync().awaitTerminated(5, TimeUnit.SECONDS));
            }

            // Clear threads.
            receiveThreads = null;
        }

        private static class InternalInterruptedException extends RuntimeException {
            public InternalInterruptedException(Throwable cause) {
                super(cause);
            }
        }

        /**
         *
         */
        private class ReceiveThread extends AbstractExecutionThreadService {
            private final int number;
            private Thread thread;

            public ReceiveThread(int number) {
                this.number = number;
            }

            @Override
            protected String serviceName() {
                return actionProvider.name + "-" + number;
            }

            @Override
            protected void triggerShutdown() {
                Try.run(() -> thread.interrupt());
            }

            protected ReceiveMessageResult receiveMessage(ReceiveMessageRequest request)
                throws InterruptedException, SocketException {
                // Receive blocking.
                return sqsBuffer.receiveMessageSync(request);
            }

            protected void deleteMessage(String receiptHandle) throws InterruptedException, SocketException {
                // Delete asynchronously.
                // Allow deletes to be batched which can increase performance quite a bit while decreasing
                // resources used and allowing allowing the next receive to happen immediately.
                // The visibility timeout buffer provides a good safety net.
                sqsBuffer.deleteMessage(new DeleteMessageRequest(queueUrl, receiptHandle), new AsyncHandler<DeleteMessageRequest, DeleteMessageResult>() {
                    @Override
                    public void onError(Exception exception) {
                        LOG.error("Failed to delete message for receipt handle " + receiptHandle, exception);
                        deleteFailuresCounter.inc();
                    }

                    @Override
                    public void onSuccess(DeleteMessageRequest request, DeleteMessageResult deleteMessageResult) {
                        // Ignore.
                    }
                });
            }

            @Override
            @SuppressWarnings("all")
            protected void run() throws Exception {
                thread = Thread.currentThread();
                receiveThreadsCounter.inc();

                try {
                    while (isRunning()) {
                        try {
                            ReceiveMessageResult result = null;
                            try {
                                lastPoll = Math.max(System.currentTimeMillis(), lastPoll);

                                final ReceiveMessageRequest request = new ReceiveMessageRequest()
                                    .withQueueUrl(queueUrl)
                                    //                                .withWaitTimeSeconds(20)
                                    //                                .withVisibilityTimeout((int) TimeUnit.MILLISECONDS.toSeconds(actionProvider.getTimeoutMillis() + 1000))
                                    .withMaxNumberOfMessages(1);

                                // Receive a batch of messages.
                                result = receiveMessage(request);
                            }
                            catch (InterruptedException e) {
                                LOG.warn("Interrupted.", e);
                                return;
                            }
                            catch (Exception e) {
                                LOG.warn("SQS Consumer Exception", e);
                            }

                            if (result == null) {
                                continue;
                            }

                            final List<Message> messages = result.getMessages();

                            // Were any messages received?
                            if (messages == null || messages.isEmpty()) {
                                continue;
                            }

                            // Are we still running.
                            if (!isRunning()) {
                                LOG.warn("Consumer not running when checked.");
                                return;
                            }

                            for (Message message : messages) {
                                // Parse request.
                                final String body = Strings.nullToEmpty(message.getBody());
                                final Object in = body.isEmpty()
                                                  ? null
                                                  : WireFormat.parse(
                                                      actionProvider.getInClass(),
                                                      body
                                                  );

                                try {
                                    final Object shouldDelete = actionProvider.execute(in);

                                    if (shouldDelete == Boolean.TRUE) {
                                        completesCounter.inc();
                                        deletesCounter.inc();

                                        try {
                                            deleteMessage(message.getReceiptHandle());
                                        }
                                        catch (Throwable e) {
                                            LOG.error("Delete message from queue '" + queueUrl + "' with receipt handle '" + message.getReceiptHandle() + "' failed", e);
                                            deleteFailuresCounter.inc();
                                        }
                                    }
                                    else {
                                        inCompletesCounter.inc();
                                    }
                                }
                                catch (Exception e) {
                                    LOG.error("Action " + actionProvider.getActionClass().getCanonicalName() + " threw an exception", e);

                                    final Throwable rootCause = Throwables.getRootCause(e);
                                    if (rootCause instanceof HystrixTimeoutException || rootCause instanceof TimeoutException) {
                                        timeoutsCounter.inc();
                                    }
                                    else {
                                        exceptionsCounter.inc();
                                    }
                                }
                            }
                        }
                        catch (Exception e) {
                            if (e instanceof InternalInterruptedException) {
                                LOG.info("Consumer Interrupted... Shutting down.");
                                return;
                            }

                            // Ignore.
                            LOG.error("ReceiveThread.run() threw an exception", e);
                        }
                    }
                }
                finally {
                    receiveThreadsCounter.dec();
                }
            }
        }
    }

    /**
     *
     */
    private final class QueueContext {
        final Sender sender;
        final Receiver receiver;
        final SQSQueueConfig config;

        public QueueContext(Sender sender, Receiver receiver, SQSQueueConfig config) {
            this.sender = sender;
            this.receiver = receiver;
            this.config = config;
        }
    }
}
