package io.clickhandler.action;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.*;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.AbstractIdleService;
import io.clickhandler.common.WireFormat;
import javaslang.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Processes Worker requests from a single AmazonSQS Queue.
 * Supports parallel receive and delete threads and utilizes SQS batching to increase message throughput.
 * <p>
 * Reliable
 */
public class SQSConsumer extends AbstractIdleService {
    public static final int VISIBILITY_BUFFER_MILLIS = 5000;
    private static final Logger LOG = LoggerFactory.getLogger(SQSConsumer.class);
    private SQSWorkerConfig config;
    private AmazonSQSClient sqsClient;
    private Semaphore buffered;
    private ReceiveThread[] receiveThreads;
    private DeleteThread[] deleteThreads;
    private int batchSize = 10;
    private int minimumVisibility = 30;
    private String queueUrl;

    private Map<String, ActionContext> actionMap;

    @Inject
    public SQSConsumer() {
    }

    /**
     * @param config
     */
    void setConfig(SQSWorkerConfig config) {
        Preconditions.checkNotNull(config, "SQSWorkerConfig cannot be null");
        Preconditions.checkArgument(config.batchSize > 0, "SQSWorkerConfig.batchSize must be greater than 0");
        Preconditions.checkArgument(config.minimumVisibility > 0, "SQSWorkerConfig.minimumVisibility must be greater than 0");
        if (config.deleteThreads < 1)
            config.deleteThreads = config.receiveThreads;
        Preconditions.checkArgument(config.deleteThreads > 0, "SQSWorkerConfig.deleteThreads must be greater than 0");
        Preconditions.checkArgument(config.receiveThreads > 0, "SQSWorkerConfig.receiveThreads must be greater than 0");
        this.config = config;
        this.batchSize = config.batchSize;
        this.minimumVisibility = config.minimumVisibility;
    }

    /**
     * @param queueUrl
     */
    void setQueueUrl(String queueUrl) {
        this.queueUrl = queueUrl;
    }

    /**
     * @param sqsClient
     */
    void setSqsClient(AmazonSQSClient sqsClient) {
        this.sqsClient = sqsClient;
    }

    @Override
    protected void startUp() throws Exception {
        // Create buffer semaphore.
        buffered = new Semaphore(config.bufferSize);

        // Init ActionQueues.
        actionMap = ActionManager.getWorkerActionMap().values().stream()
            .map(ActionContext::new)
            .collect(Collectors.toMap(k -> k.actionProvider.getActionClass().getCanonicalName(), v -> v));

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
        // Stop receiving messages.
        for (ReceiveThread thread : receiveThreads) {
            Try.run(() -> thread.stopAsync().awaitTerminated());
        }

        // Wait for all actions to stop.
        buffered.acquire(config.bufferSize);

        // Delete any remaining messages.
        // Stop delete threads.
        for (DeleteThread thread : deleteThreads) {
            Try.run(() -> thread.stopAsync().awaitTerminated());
        }

        // Clear threads.
        receiveThreads = null;
        deleteThreads = null;
    }

    /**
     * @param take
     * @return
     */
    private boolean deleteRun(LinkedBlockingDeque<String> queue, boolean take) {
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
                sqsClient.deleteMessageBatch(new DeleteMessageBatchRequest()
                    .withQueueUrl(queueUrl)
                    .withEntries(batch));
                return true;
            } else {
                return false;
            }
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

    /**
     * Delete threads each have their own queue of ReceiptHandles to delete.
     */
    private class DeleteThread extends AbstractExecutionThreadService {
        private final LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();

        @Override
        protected void shutDown() throws Exception {
            while (deleteRun(queue, false)) ;
        }

        @Override
        protected void run() throws Exception {
            while (isRunning()) {
                try {
                    deleteRun(queue, true);
                } catch (Throwable e) {
                    // Ignore.
                    LOG.error("DeleteThread.run threw an exception", e);
                }
            }
        }
    }

    /**
     *
     */
    private class ReceiveThread extends AbstractExecutionThreadService {
        @Override
        protected void run() throws Exception {
            while (isRunning()) {
                try {
                    ReceiveMessageResult result = null;
                    buffered.acquire(batchSize);
                    try {
                        if (!isRunning()) {
                            buffered.release(batchSize);
                            return;
                        }

                        // Receive a batch of messages.
                        result = sqsClient.receiveMessage(new ReceiveMessageRequest()
                            .withQueueUrl(queueUrl)
                            .withWaitTimeSeconds(20)
                            .withVisibilityTimeout(minimumVisibility)
                            .withMaxNumberOfMessages(batchSize));
                    } catch (Throwable e) {
                        buffered.release(batchSize);
                        Throwables.propagate(e);
                    }

                    // Were any messages received?
                    if (result == null || result.getMessages() == null || result.getMessages().isEmpty()) {
                        buffered.release(batchSize);
                        continue;
                    }

                    // Release extra permits.
                    if (result.getMessages().size() < batchSize) {
                        buffered.release(batchSize - result.getMessages().size());
                    }

                    List<ChangeMessageVisibilityBatchRequestEntry> changes = null;
                    for (Message message : result.getMessages()) {
                        final MessageAttributeValue type = message.getMessageAttributes() != null
                            ? message.getMessageAttributes().get(SQSService.ATTRIBUTE_TYPE)
                            : null;

                        if (type == null) {
                            buffered.release();
                            scheduleToDelete(message.getReceiptHandle());
                            continue;
                        }

                        // Get action name.
                        final String actionType = Try.of(type::getStringValue).getOrElse("");
                        if (actionType == null || actionType.isEmpty()) {
                            buffered.release();
                            scheduleToDelete(message.getReceiptHandle());
                            continue;
                        }

                        // Find ActionContext
                        final ActionContext actionContext = actionMap.get(actionType);
                        if (actionContext == null) {
                            buffered.release();
                            scheduleToDelete(message.getReceiptHandle());
                            continue;
                        }

                        // Parse request.
                        final String body = Strings.nullToEmpty(message.getBody());
                        final Object in = body.isEmpty() ? null : WireFormat.parse(
                            actionContext.actionProvider.getInClass(),
                            body
                        );

                        // Create ActionRequest.
                        final ActionRequest request = new ActionRequest(message.getReceiptHandle(), in);

                        // Schedule to Run.
                        int extendBySeconds = actionContext.schedule(request);

                        // Do we need to extend the visibility timeout?
                        if (extendBySeconds > 0) {
                            if (changes == null) {
                                changes = new ArrayList<>(10);
                            }
                            changes.add(new ChangeMessageVisibilityBatchRequestEntry(
                                Integer.toString(changes.size()),
                                message.getReceiptHandle()
                            ));
                        }
                    }

                    if (changes != null) {
                        final ChangeMessageVisibilityBatchResult visibilityBatchResult =
                            sqsClient.changeMessageVisibilityBatch(new ChangeMessageVisibilityBatchRequest()
                                .withQueueUrl(queueUrl)
                                .withEntries(changes)
                            );
                    }
                } catch (Throwable e) {
                    LOG.error("ReceiveThread.run() threw an exception", e);
                }
            }
        }
    }

    /**
     * Manages executing one Action type.
     * <p>
     * If the Action has maxConcurrentRequests set, then a backlog is created
     * and the SQS message visibility may be extended by a configurable multiple
     * of the maxExecutionMillis that's configured for the Action type.
     */
    private final class ActionContext {
        private final AtomicInteger running = new AtomicInteger(0);
        private final WorkerActionProvider actionProvider;
        private final int maxConcurrentRequests;
        private final long executionMillis;
        private final int changeVisibilityBy;
        private final int changeVisibilityByQueue;
        private final LinkedList<ActionRequest> backlog;

        private ActionContext(WorkerActionProvider actionProvider) {
            this.actionProvider = actionProvider;
            this.maxConcurrentRequests = actionProvider.getMaxConcurrentRequests();
            this.executionMillis = actionProvider.getTimeoutMillis();

            // Should we accept a backlog?
            if (maxConcurrentRequests > 0) {
                this.backlog = new LinkedList<>();
            } else {
                this.backlog = null;
            }

            if (executionMillis >= TimeUnit.SECONDS.toMillis(minimumVisibility) - VISIBILITY_BUFFER_MILLIS) {
                changeVisibilityBy = (int) TimeUnit.MILLISECONDS.toSeconds(
                    executionMillis + VISIBILITY_BUFFER_MILLIS - TimeUnit.SECONDS.toMillis(minimumVisibility)
                );
            } else {
                changeVisibilityBy = 0;
            }

            if (executionMillis > (TimeUnit.SECONDS.toMillis(minimumVisibility) / 2)) {
                changeVisibilityByQueue = (int) TimeUnit.MILLISECONDS.toSeconds(
                    executionMillis * 2 - TimeUnit.SECONDS.toMillis(minimumVisibility)
                );
            } else {
                changeVisibilityByQueue = 0;
            }
        }

        /**
         * @param request
         * @return
         */
        private int schedule(final ActionRequest request) {
            if (backlog != null) {
                synchronized (this) {
                    if (maxConcurrentRequests > 0 && running.get() >= maxConcurrentRequests) {
                        backlog.add(request);
                        return changeVisibilityByQueue;
                    }
                    running.incrementAndGet();
                }
            } else {
                running.incrementAndGet();
            }

            run(request);
            return changeVisibilityBy;
        }

        /**
         * @param request
         */
        private void run(ActionRequest request) {
            final long duration = System.currentTimeMillis() - request.created;

            actionProvider.observe(request.in).subscribe(
                r -> {
                    try {
                        // Do we need to remove from the Queue?
                        if (r == Boolean.TRUE) {
                            scheduleToDelete(request.receiptHandle);
                        }
                    } finally {
                        buffered.release();

                        if (backlog != null) {
                            synchronized (this) {
                                final ActionRequest next = backlog.poll();
                                if (next != null) {
                                    run(next);
                                } else {
                                    running.decrementAndGet();
                                }
                            }
                        } else {
                            running.decrementAndGet();
                        }
                    }
                },
                e -> {
                    try {
                        LOG.warn("Action [" + actionProvider.getName() + "] threw an exception. Placed back on the Queue.", e);
                    } finally {
                        try {
                            // Do not
                            buffered.release();
                        } finally {
                            running.decrementAndGet();
                        }
                    }
                }
            );
        }
    }

    /**
     *
     */
    private final class ActionRequest {
        final long created = System.currentTimeMillis();
        final String receiptHandle;
        final Object in;
        long timeoutAt;

        public ActionRequest(String receiptHandle, Object in) {
            this.receiptHandle = receiptHandle;
            this.in = in;
        }
    }
}
