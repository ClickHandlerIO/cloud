package move.action;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.*;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.netflix.hystrix.exception.HystrixTimeoutException;
import io.vertx.rxjava.core.Context;
import io.vertx.rxjava.core.Vertx;
import javaslang.control.Try;
import move.common.Metrics;
import move.common.WireFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Subscription;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Processes Worker requests from a single AmazonSQS Queue.
 * Supports parallel receive and delete threads and utilizes SQS batching to increase message throughput.
 * <p>
 * Reliable
 */
public class SQSConsumer extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(SQSConsumer.class);
    private final Vertx vertx;
    private LinkedBlockingDeque<ActionRequest> requestQueue;
    private SQSWorkerConfig config;
    private AmazonSQS sqsDeleteClient;
    private AmazonSQS sqsReceiveClient;
    private ReceiveThread[] receiveThreads;
    private DeleteThread[] deleteThreads;
    private int batchSize = 1;
    private WorkerActionProvider actionProvider;
    private String queueUrl;
    private ConcurrentMap<String, ActionRequest> inFlightMap = new ConcurrentHashMap<>();
    private DispatchService dispatchService;
    private TimeoutService timeoutService;

    private Gauge<Long> inFlightGauge;
    private Gauge<Long> secondsSinceLastPollGauge;
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
        Preconditions.checkArgument(
            config.batchSize > 0,
            "SQSWorkerConfig.batchSize must be greater than 0"
        );
        if (config.deleteThreads < 1)
            config.deleteThreads = config.receiveThreads;
        Preconditions.checkArgument(
            config.deleteThreads > 0,
            "SQSWorkerConfig.deleteThreads must be greater than 0"
        );
        Preconditions.checkArgument(
            config.receiveThreads > 0,
            "SQSWorkerConfig.receiveThreads must be greater than 0"
        );
        this.config = config;
        this.batchSize = config.batchSize;
    }

    /**
     * @param queueUrl
     */
    void setQueueUrl(String queueUrl) {
        this.queueUrl = queueUrl;
    }

    void setSqsReceiveClient(AmazonSQS sqsReceiveClient) {
        this.sqsReceiveClient = sqsReceiveClient;
    }

    /**
     * @param sqsDeleteClient
     */
    void setSqsDeleteClient(AmazonSQS sqsDeleteClient) {
        this.sqsDeleteClient = sqsDeleteClient;
    }

    void setActionProvider(WorkerActionProvider actionProvider) {
        this.actionProvider = actionProvider;
    }

    @Override
    protected void startUp() throws Exception {
        LOG.debug("Starting up SQS service.");

        final String name = Strings.nullToEmpty(config.name).trim();

        requestQueue = new LinkedBlockingDeque<>(batchSize);

        // Find ActionProvider.
        Preconditions.checkNotNull(actionProvider, "ActionProvider for '" + name + "' was not set.");

        final MetricRegistry registry = Metrics.registry();
        inFlightGauge = registry.register(
            config.name + "-IN_FLIGHT",
            () -> (long) inFlightMap.size()
        );
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

        dispatchService = new DispatchService();
        timeoutService = new TimeoutService();

        dispatchService.startAsync().awaitRunning();
        timeoutService.startAsync().awaitRunning();

        // Start Delete threads.
        deleteThreads = new DeleteThread[config.deleteThreads];
        for (int i = 0; i < deleteThreads.length; i++) {
            deleteThreads[i] = new DeleteThread();
            deleteThreads[i].startAsync().awaitRunning();
        }

        // Start Receive threads.
        receiveThreads = new ReceiveThread[config.receiveThreads];
        for (int i = 0; i < receiveThreads.length; i++) {
            receiveThreads[i] = new ReceiveThread();
            receiveThreads[i].startAsync().awaitRunning();
        }
    }

    @Override
    protected void shutDown() throws Exception {
        Try.run(() -> sqsReceiveClient.shutdown());

        // Stop receiving messages.
        for (ReceiveThread thread : receiveThreads) {
            Try.run(() -> thread.stopAsync().awaitTerminated(5, TimeUnit.SECONDS));
        }

        // Delete any remaining messages.
        // Stop delete threads.
        for (DeleteThread thread : deleteThreads) {
            Try.run(() -> thread.stopAsync().awaitTerminated(5, TimeUnit.SECONDS));
        }

        // Delete client.
        Try.run(() -> sqsDeleteClient.shutdown());

        // Clear threads.
        receiveThreads = null;
        deleteThreads = null;
    }

    private DeleteMessageBatchResult deleteMessageBatch(DeleteMessageBatchRequest request)
        throws InterruptedException, SocketException {
        return sqsDeleteClient.deleteMessageBatch(request);
    }

    /**
     * @param take
     * @return
     */
    private boolean deleteRun(LinkedBlockingDeque<String> queue, boolean take)
        throws InterruptedException, IOException {
        try {
            final ArrayList<DeleteMessageBatchRequestEntry> batch = new ArrayList<>(batchSize);
            final ArrayList<String> takeBatch = new ArrayList<>(batchSize);

            int size = queue.drainTo(takeBatch, batchSize);
            if (size == 0) {
                String receiptHandle = take ? queue.take() : queue.poll();
                if (receiptHandle == null)
                    return false;

                takeBatch.add(receiptHandle);
                if (batchSize > 1)
                    queue.drainTo(takeBatch, batchSize - 1);
            }

            for (int i = 0; i < takeBatch.size(); i++) {
                batch.add(new DeleteMessageBatchRequestEntry(Integer.toString(i), takeBatch.get(i)));
            }

            if (!batch.isEmpty()) {
                deletesCounter.inc(batch.size());
                DeleteMessageBatchResult result = deleteMessageBatch(new DeleteMessageBatchRequest()
                    .withQueueUrl(queueUrl)
                    .withEntries(batch));

                if (result.getFailed() != null && !result.getFailed().isEmpty()) {
                    deleteFailuresCounter.inc(result.getFailed().size());
                }

                return true;
            } else {
                return false;
            }
        } catch (SocketException e) {
            // Ignore.
        } catch (AmazonClientException e) {
            return false;
        } catch (InterruptedException e) {
            return false;
        } catch (Throwable e) {
            LOG.error("AmazonSQSClient.deleteMessageBatch() threw an exception", e);
        }

        return false;
    }

    /**
     * Schedules a message to be deleted from SQS.
     * Uses a consistent hash on the ReceiptHandle to pick a thread.
     *
     * @param receiptHandle ReceiptHandle to delete.
     */
    private void scheduleToDelete(String receiptHandle) {
        try {
            // Do we only have 1 thread?
            if (deleteThreads.length == 1) {
                deleteThreads[0].queue.add(receiptHandle);
                return;
            }
            deleteThreads[Hashing.consistentHash(
                Hashing.adler32().hashString(receiptHandle, Charsets.UTF_8),
                deleteThreads.length
            )].queue.add(receiptHandle);
        } catch (Throwable e) {
            LOG.error("scheduleToDelete threw an exception", e);
        }
    }

    private static class InternalInterruptedException extends RuntimeException {
        public InternalInterruptedException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Delete threads each have their own queue of ReceiptHandles to delete.
     */
    private class DeleteThread extends AbstractExecutionThreadService {
        private final LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
        private Thread thread;

        @Override
        protected String serviceName() {
            return "worker-consumer-delete-" + config.name + "-SQS-" + config.sqsName;
        }

        @Override
        protected void triggerShutdown() {
            Try.run(() -> thread.interrupt());
        }

        @Override
        protected void shutDown() throws Exception {
            try {
                while (deleteRun(queue, false)) ;
            } catch (InterruptedException e) {
                // Ignore.
            }
        }

        @Override
        protected void run() throws Exception {
            thread = Thread.currentThread();
            deleteThreadsCounter.inc();

            try {
                while (isRunning()) {
                    try {
                        deleteRun(queue, true);
                    } catch (InterruptedException e) {
                        return;
                    } catch (Throwable e) {
                        // Ignore.
                        LOG.error("DeleteThread.run threw an exception", e);
                    }
                }
            } finally {
                deleteThreadsCounter.dec();
            }
        }
    }

    /**
     *
     */
    private class ReceiveThread extends AbstractExecutionThreadService {
        private Thread thread;
        private Context context;

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
            return sqsReceiveClient.receiveMessage(request);
        }

        @Override
        protected void run() throws Exception {
            thread = Thread.currentThread();
            context = vertx.getOrCreateContext();
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
                                .withMaxNumberOfMessages(batchSize);

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

                        messages.stream().map(message -> {
                                // Parse request.
                                final String body = Strings.nullToEmpty(message.getBody());
                                final Object in = body.isEmpty() ? null : WireFormat.parse(
                                    actionProvider.getInClass(),
                                    body
                                );

                                // Create ActionRequest.
                                final ActionRequest request = new ActionRequest(
                                    context,
                                    message.getReceiptHandle(),
                                    in,
                                    System.currentTimeMillis() + actionProvider.getTimeoutMillis()
                                );

                                return request;
                            }
                        ).filter(
                            $ -> {
                                if (inFlightMap.putIfAbsent($.receiptHandle, $) == null) {
                                    jobsCounter.inc();
                                    return true;
                                }

                                return false;
                            }
                        ).forEach(
                            $ -> {
                                try {
                                    requestQueue.put($);
                                } catch (InterruptedException e) {
                                    throw new InternalInterruptedException(e);
                                }
                            }
                        );
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

    /**
     * Dispatches Action Request.
     */
    private final class DispatchService extends AbstractExecutionThreadService {
        @Override
        protected void run() throws Exception {
            while (isRunning()) {
                try {
                    final ActionRequest actionRequest = requestQueue.poll(1, TimeUnit.SECONDS);
                    if (actionRequest == null) {
                        continue;
                    }

                    actionRequest.invoke();
                } catch (Throwable e) {
                    if (e instanceof InterruptedException) {
                        return;
                    }
                }
            }
        }
    }

    /**
     * Ensures In-Flight messages are cleaned up.
     */
    private final class TimeoutService extends AbstractScheduledService {
        @Override
        protected void runOneIteration() throws Exception {
            try {
                final long now = System.currentTimeMillis();
                Iterator<Map.Entry<String, ActionRequest>> iterator = inFlightMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    final Map.Entry<String, ActionRequest> entry = iterator.next();

                    final ActionRequest request = entry.getValue();
                    if (request == null) {
                        continue;
                    }

                    if (request.timedOut(now)) {
                        iterator.remove();
                    }
                }
            } catch (Throwable e) {
                LOG.error("Unexpected exception thrown", e);
            }
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedRateSchedule(0, 1, TimeUnit.SECONDS);
        }
    }

    /**
     *
     */
    private final class ActionRequest {
        final Context context;
        final String receiptHandle;
        final Object in;
        final long timeoutAt;
        final AtomicBoolean done = new AtomicBoolean(false);
        Subscription subscription;

        public ActionRequest(Context context,
                             String receiptHandle,
                             Object in,
                             long timeoutAt) {
            this.context = context;
            this.receiptHandle = receiptHandle;
            this.in = in;
            this.timeoutAt = timeoutAt;
        }

        boolean timedOut(long now) {
            if (done.get()) {
                if (subscription != null && !subscription.isUnsubscribed()) {
                    Try.run(() -> subscription.unsubscribe());
                }
                timeoutsCounter.inc();
                return true;
            }

            if (now <= timeoutAt) {
                Try.run(() -> subscription.unsubscribe());
                return true;
            }

            return false;
        }

        @SuppressWarnings("all")
        void invoke() {
            subscription = actionProvider.observe(in).subscribe(
                r -> {
                    if (done.compareAndSet(false, true)) {
                        inFlightMap.remove(receiptHandle);

                        try {
                            if (r == Boolean.TRUE) {
                                completesCounter.inc();
                                scheduleToDelete(receiptHandle);
                            } else {
                                inCompletesCounter.inc();
                            }
                        } finally {
                            Try.run(() -> subscription.unsubscribe());
                        }
                    }
                },
                e -> {
                    inFlightMap.remove(receiptHandle);

                    if (done.compareAndSet(false, true)) {
                        try {
                            if (e instanceof HystrixTimeoutException) {
                                timeoutsCounter.inc();
                            } else {
                                exceptionsCounter.inc();
                            }
                        } finally {
                            Try.run(() -> subscription.unsubscribe());
                        }
                    }
                    LOG.error(
                        "Action " + actionProvider.getActionClass().getCanonicalName() + " threw an exception", e);
                }
            );
        }
    }
}
