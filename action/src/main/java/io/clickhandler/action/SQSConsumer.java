package io.clickhandler.action;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.*;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.AbstractIdleService;
import io.clickhandler.common.WireFormat;
import io.vertx.rxjava.core.Context;
import io.vertx.rxjava.core.Vertx;
import javaslang.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketException;
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
    private static final Logger LOG = LoggerFactory.getLogger(SQSConsumer.class);
    private final Vertx vertx;
    private SQSWorkerConfig config;
    private AmazonSQS sqsDeleteClient;
    private AmazonSQS sqsReceiveClient;
    private Semaphore buffered;
    private ReceiveThread[] receiveThreads;
    private DeleteThread[] deleteThreads;
    private int batchSize = 10;
    private int minimumVisibility = 30;
    private int visibilityBufferMillis = 5_000;
    private String queueUrl;
    private Map<String, ActionContext> actionMap;
    private ActionContext dedicated;

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
        Preconditions.checkArgument(
            config.minimumVisibility > 0,
            "SQSWorkerConfig.minimumVisibility must be greater than 0"
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
        this.minimumVisibility = config.minimumVisibility;

        if (config.visibilityBuffer < 1 || config.visibilityBuffer > 10) {
            this.visibilityBufferMillis = 5_000;
            LOG.warn("SQSWorkerConfig[" +
                config.name +
                "].visibilityBuffer must be between 1 and 10. Value attempted was " +
                config.visibilityBuffer +
                ". Setting to 5 seconds.");
        } else {
            this.visibilityBufferMillis = (int) TimeUnit.SECONDS.toMillis(config.visibilityBuffer);
        }
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

    void setDedicated(WorkerActionProvider dedicated) {
        this.dedicated = new ActionContext(dedicated);
    }

    @Override
    protected void startUp() throws Exception {
        // Create buffer semaphore.
        buffered = new Semaphore(config.bufferSize);

        // Init ActionQueues.
        actionMap = ActionManager.getWorkerActionMap().values().stream()
            .map(ActionContext::new)
            .collect(Collectors.toMap(
                k -> k.actionProvider.getActionClass().getCanonicalName(),
                v -> v,
                (v1, v2) -> v2)
            );

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

        buffered.acquire(config.bufferSize);

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
                deleteMessageBatch(new DeleteMessageBatchRequest()
                    .withQueueUrl(queueUrl)
                    .withEntries(batch));
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

    protected ActionContext getActionContext(Message message) {
        if (dedicated != null)
            return dedicated;

        final MessageAttributeValue type = message.getMessageAttributes() != null
            ? message.getMessageAttributes().get(SQSService.ATTRIBUTE_NAME)
            : null;

        if (type == null) {
            return null;
        }

        // Get action name.
        final String actionType = Try.of(type::getStringValue).getOrElse("");
        if (actionType == null || actionType.isEmpty()) {
            return null;
        }

        return actionMap.get(actionType);
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

        protected ChangeMessageVisibilityBatchResult changeVisibility(ChangeMessageVisibilityBatchRequest request)
            throws InterruptedException, SocketException {
            return sqsReceiveClient.changeMessageVisibilityBatch(request);
        }

        @Override
        protected void run() throws Exception {
            thread = Thread.currentThread();
            context = vertx.getOrCreateContext();

            while (isRunning()) {
                try {
                    ReceiveMessageResult result = null;
                    buffered.acquire(batchSize);
                    if (!isRunning()) {
                        buffered.release(batchSize);
                        return;
                    }

                    try {
                        final ReceiveMessageRequest request = new ReceiveMessageRequest()
                            .withQueueUrl(queueUrl)
                            .withWaitTimeSeconds(20)
                            .withVisibilityTimeout(minimumVisibility)
                            .withMaxNumberOfMessages(batchSize);

                        if (dedicated == null) {
                            request.withMessageAttributeNames(SQSService.ATTRIBUTE_NAME);
                        }

                        // Receive a batch of messages.
                        result = receiveMessage(request);
                    } catch (SocketException e) {
                        buffered.release(batchSize);
                    } catch (InterruptedException e) {
                        buffered.release(batchSize);
                        return;
                    } catch (AmazonClientException e) {
                        buffered.release(batchSize);
                    } catch (Throwable e) {
                        buffered.release(batchSize);

                        if (!isRunning() && e instanceof SocketException) {
                            return;
                        }

                        Throwables.propagate(e);
                    }

                    if (!isRunning())
                        return;

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
                        // Find ActionContext
                        final ActionContext actionContext = getActionContext(message);
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
                        final ActionRequest request = actionContext.createRequest(
                            context,
                            message.getReceiptHandle(),
                            in
                        );

                        try {
                            // Do we need to extend the visibility timeout?
                            if (request.extendBySeconds > 0) {
                                if (changes == null) {
                                    changes = new ArrayList<>(batchSize);
                                }
                                changes.add(new ChangeMessageVisibilityBatchRequestEntry(
                                    Integer.toString(changes.size()),
                                    message.getReceiptHandle()
                                ));
                            }
                        } finally {
                            if (!request.queued) {
                                try {
                                    actionContext.run(request);
                                } catch (InterruptedException e) {
                                    return;
                                }
                            }
                        }
                    }

                    if (changes != null) {
                        try {
                            final ChangeMessageVisibilityBatchResult visibilityBatchResult =
                                changeVisibility(new ChangeMessageVisibilityBatchRequest()
                                    .withQueueUrl(queueUrl)
                                    .withEntries(changes)
                                );
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                } catch (SocketException e) {
                    // Ignore.
                } catch (AmazonClientException e) {
                    // Ignore.
                } catch (InterruptedException e) {
                    return;
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

            if (executionMillis >= TimeUnit.SECONDS.toMillis(minimumVisibility) - visibilityBufferMillis) {
                changeVisibilityBy = (int) TimeUnit.MILLISECONDS.toSeconds(
                    executionMillis + visibilityBufferMillis - TimeUnit.SECONDS.toMillis(minimumVisibility)
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
         * @return
         */
        private ActionRequest createRequest(Context context, final String receiptHandle, Object in) {
            if (backlog != null) {
                synchronized (this) {
                    if (maxConcurrentRequests > 0 && running.get() >= maxConcurrentRequests) {
                        final ActionRequest request = new ActionRequest(
                            context,
                            receiptHandle,
                            in,
                            true,
                            changeVisibilityByQueue,
                            actionProvider.getTimeoutMillis()
                        );
                        backlog.add(request);
                        return request;
                    }
                    running.incrementAndGet();
                }
            } else {
                running.incrementAndGet();
            }

            return new ActionRequest(
                context,
                receiptHandle,
                in,
                false,
                changeVisibilityBy,
                actionProvider.getTimeoutMillis()
            );
        }

        /**
         * @param request
         */
        private void run(ActionRequest request) throws InterruptedException {
            final long worstCaseTime = System.currentTimeMillis() + actionProvider.getTimeoutMillis();

            // Is there enough time leased to reliably run this Action?
            if (request.timeoutAt > worstCaseTime + visibilityBufferMillis) {
                try {
                    buffered.release();
                } finally {
                    dequeue();
                }
                return;
            }

            request.context.runOnContext(a -> actionProvider.observe(request.in).subscribe(
                r -> {
                    request.context.runOnContext(a2 -> {
                        try {
                            // Do we need to remove from the Queue?
                            if (r == Boolean.TRUE) {
                                scheduleToDelete(request.receiptHandle);
                            }
                        } finally {
                            try {
                                buffered.release();
                            } finally {
                                dequeue();
                            }
                        }
                    });
                },
                e -> {
                    request.context.runOnContext(a2 -> {
                        try {
                            LOG.warn(
                                "Action [" +
                                    actionProvider.getName() +
                                    "] threw an exception. Placed back on the Queue.",
                                e
                            );
                        } finally {
                            try {
                                buffered.release();
                            } finally {
                                dequeue();
                            }
                        }
                    });
                }
            ));
        }

        private void dequeue() {
            if (backlog != null) {
                synchronized (this) {
                    final ActionRequest next = backlog.poll();
                    if (next != null) {
                        try {
                            run(next);
                        } catch (InterruptedException e) {
                            return;
                        }
                    } else {
                        running.decrementAndGet();
                    }
                }
            } else {
                running.decrementAndGet();
            }
        }
    }

    /**
     *
     */
    private final class ActionRequest {
        final long created = System.currentTimeMillis();
        final Context context;
        final String receiptHandle;
        final Object in;
        final boolean queued;
        final int extendBySeconds;
        final long timeoutAt;

        public ActionRequest(Context context,
                             String receiptHandle,
                             Object in,
                             boolean queued,
                             int extendBySeconds,
                             long timeoutAt) {
            this.context = context;
            this.receiptHandle = receiptHandle;
            this.in = in;
            this.queued = queued;
            this.extendBySeconds = extendBySeconds;
            this.timeoutAt = timeoutAt;
        }
    }
}
