package move.action;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteMessageResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.AbstractIdleService;
import com.netflix.hystrix.exception.HystrixTimeoutException;
import io.vertx.rxjava.core.Vertx;
import javaslang.control.Try;
import move.common.Metrics;
import move.common.WireFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Processes Worker requests from a single AmazonSQS Queue.
 * Supports parallel receive and delete threads and utilizes SQS batching to increase message throughput.
 * <p>
 * Reliable
 */
public class SQSConsumer extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(SQSConsumer.class);
    private final Vertx vertx;
    private SQSWorkerConfig config;
    private AmazonSQS sqlClient;
    private ReceiveThread[] receiveThreads;
    private WorkerActionProvider actionProvider;
    private String queueUrl;

    private Gauge<Long> secondsSinceLastPollGauge;
    private Gauge<Long> permitsGauge;
    private Counter receiveThreadsCounter;
    private Counter deleteThreadsCounter;
    private Counter jobsCounter;
    private Counter timeoutsCounter;
    private Counter completesCounter;
    private Counter inCompletesCounter;
    private Counter exceptionsCounter;
    private Counter deletesCounter;
    private Counter deleteFailuresCounter;

    private volatile long lastPoll;

    public SQSConsumer(Vertx vertx) {
        this.vertx = vertx;
    }

    /**
     * @param config
     */
    void setConfig(SQSWorkerConfig config) {
        Preconditions.checkNotNull(
            config,
            "SQSWorkerConfig cannot be null"
        );
        this.config = config;
    }

    /**
     * @param queueUrl
     */
    void setQueueUrl(String queueUrl) {
        this.queueUrl = queueUrl;
    }

    void setSqlClient(AmazonSQS sqlClient) {
        this.sqlClient = sqlClient;
    }

    void setActionProvider(WorkerActionProvider actionProvider) {
        this.actionProvider = actionProvider;
    }

    @Override
    protected void startUp() throws Exception {
        LOG.debug("Starting up SQS service.");

        final String name = Strings.nullToEmpty(config.name).trim();

        // Find ActionProvider.
        Preconditions.checkNotNull(actionProvider, "ActionProvider for '" + name + "' was not set.");

        final MetricRegistry registry = Metrics.registry();
        secondsSinceLastPollGauge = registry.register(
            config.name + "-SECONDS_SINCE_LAST_POLL",
            () -> TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastPoll)
        );
        receiveThreadsCounter = registry.counter(config.name + "-RECEIVE_THREADS");
        deleteThreadsCounter = registry.counter(config.name + "-DELETE_THREADS");
        jobsCounter = registry.counter(config.name + "-JOBS");
        completesCounter = registry.counter(config.name + "-COMPLETES");
        inCompletesCounter = registry.counter(config.name + "-IN_COMPLETES");
        exceptionsCounter = registry.counter(config.name + "-EXCEPTIONS");
        timeoutsCounter = registry.counter(config.name + "-TIMEOUTS");
        deletesCounter = registry.counter(config.name + "-DELETES");
        deleteFailuresCounter = registry.counter(config.name + "-DELETE_FAILURES");

        // Start Receive threads.
        receiveThreads = new ReceiveThread[actionProvider.getMaxConcurrentRequests()];
        for (int i = 0; i < receiveThreads.length; i++) {
            receiveThreads[i] = new ReceiveThread();
            receiveThreads[i].startAsync().awaitRunning();
        }
    }

    @Override
    protected void shutDown() throws Exception {
        Try.run(() -> sqlClient.shutdown());

        // Stop receiving messages.
        for (ReceiveThread thread : receiveThreads) {
            Try.run(() -> thread.stopAsync().awaitTerminated(5, TimeUnit.SECONDS));
        }

        // Delete client.
        Try.run(() -> sqlClient.shutdown());

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
        private Thread thread;

        @Override
        protected String serviceName() {
            return "worker-consumer-receive-" + config.name + "-SQS-" + config.sqsName;
        }

        @Override
        protected void triggerShutdown() {
            Try.run(() -> thread.interrupt());
        }

        protected ReceiveMessageResult receiveMessage(ReceiveMessageRequest request)
            throws InterruptedException, SocketException {
            return sqlClient.receiveMessage(request);
        }

        protected DeleteMessageResult deleteMessage(String receiptHandle) throws InterruptedException, SocketException {
            return sqlClient.deleteMessage(queueUrl, receiptHandle);
        }

        @Override
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
                                .withWaitTimeSeconds(20)
                                .withVisibilityTimeout((int) TimeUnit.MILLISECONDS.toSeconds(actionProvider.getTimeoutMillis() + 1000))
                                .withMaxNumberOfMessages(1);

                            // Receive a batch of messages.
                            LOG.info("Trying to get messages");
                            result = receiveMessage(request);
                        } catch (InterruptedException e) {
                            LOG.warn("Interrupted.", e);
                            return;
                        } catch (Exception e) {
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
                            final Object in = body.isEmpty() ? null : WireFormat.parse(
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
                                    } catch (Throwable e) {
                                        LOG.error("Delete message from queue '" + queueUrl + "' with receipt handle '" + message.getReceiptHandle() + "' failed", e);
                                        deleteFailuresCounter.inc();
                                    }
                                } else {
                                    inCompletesCounter.inc();
                                }
                            } catch (Exception e) {
                                LOG.error("Action " + actionProvider.getActionClass().getCanonicalName() + " threw an exception", e);

                                if (e.getCause() instanceof HystrixTimeoutException) {
                                    timeoutsCounter.inc();
                                } else {
                                    exceptionsCounter.inc();
                                }
                            }
                        }
                    } catch (Exception e) {
                        if (e instanceof InternalInterruptedException) {
                            LOG.info("Consumer Interrupted... Shutting down.");
                            return;
                        }

                        // Ignore.
                        LOG.error("ReceiveThread.run() threw an exception", e);
                    }
                }
            } finally {
                receiveThreadsCounter.dec();
            }
        }
    }
}
