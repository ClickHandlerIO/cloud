package io.clickhandler.action;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.*;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.AbstractIdleService;
import io.clickhandler.common.WireFormat;
import javaslang.control.Try;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Processes Worker requests from a single AmazonSQS Queue.
 * Supports parallel receive and delete threads to increase message throughput.
 */
public class SQSWorkerReceiver extends AbstractIdleService {
    static final String ATTRIBUTE_TYPE = "t";
    static final int MAX_MESSAGES_PER_LOOP = 10;

    private final LinkedBlockingDeque<String> deleteQueue = new LinkedBlockingDeque<>();
    @Inject
    ActionManager actionManager;
    private SQSWorkerConfig config;
    private AmazonSQSClient sqsClient;
    private Semaphore buffered;
    private ReceiveThread[] receiveThreads;
    private DeleteThread[] deleteThreads;

    @Inject
    public SQSWorkerReceiver() {
    }

    public void setConfig(SQSWorkerConfig config) {
        this.config = config;
    }

    public void setSqsClient(AmazonSQSClient sqsClient) {
        this.sqsClient = sqsClient;
    }

    @Override
    protected void startUp() throws Exception {
        buffered = new Semaphore(config.bufferSize);

        receiveThreads = new ReceiveThread[config.threads];
        for (int i = 0; i < config.threads; i++) {
            receiveThreads[i] = new ReceiveThread();
            receiveThreads[i].startAsync().awaitRunning();
        }

        deleteThreads = new DeleteThread[config.threads];
        for (int i = 0; i < config.threads; i++) {
            deleteThreads[i] = new DeleteThread();
            deleteThreads[i].startAsync().awaitRunning();
        }
    }

    @Override
    protected void shutDown() throws Exception {
        // Stop receiving messages.
        for (ReceiveThread thread : receiveThreads) {
            Try.run(() -> thread.stopAsync().awaitTerminated());
        }
        // Stop delete threads.
        for (DeleteThread thread : deleteThreads) {
            Try.run(() -> thread.stopAsync().awaitTerminated());
        }

        // Wait for all actions to stop.
        buffered.acquire(config.bufferSize);

        // Delete any remaining messages.
        while (deleteRun(false)) ;

        receiveThreads = null;
        deleteThreads = null;
    }

    private DeleteMessageBatchResult delete(ArrayList<DeleteMessageBatchRequestEntry> batch) {
        final DeleteMessageBatchRequest request = new DeleteMessageBatchRequest();
        request.setEntries(batch);
        return sqsClient.deleteMessageBatch(request);
    }

    private boolean deleteRun(boolean take) {
        try {
            ArrayList<DeleteMessageBatchRequestEntry> batch = new ArrayList<>(10);
            String receiptHandle;
            while (true) {
                receiptHandle = take ? deleteQueue.take() : deleteQueue.poll();
                if (receiptHandle == null)
                    break;

                batch.add(new DeleteMessageBatchRequestEntry(Integer.toString(batch.size()), receiptHandle));

                if (batch.size() == MAX_MESSAGES_PER_LOOP) {
                    delete(batch);
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                delete(batch);
                return true;
            } else {
                return false;
            }
        } catch (Throwable e) {
            // Ignore.
        }

        return false;
    }

    private class DeleteThread extends AbstractExecutionThreadService {
        @Override
        protected void run() throws Exception {
            while (isRunning()) {
                try {
                    deleteRun(true);
                } catch (Throwable e) {
                    // Ignore.
                }
            }
        }
    }

    private class ReceiveThread extends AbstractExecutionThreadService {
        @Override
        protected void run() throws Exception {
            while (isRunning()) {
                try {
                    ReceiveMessageResult result = null;
                    buffered.acquire(MAX_MESSAGES_PER_LOOP);
                    try {
                        result = sqsClient.receiveMessage(new ReceiveMessageRequest()
                            .withWaitTimeSeconds(10)
                            .withVisibilityTimeout(60)
                            .withMaxNumberOfMessages(MAX_MESSAGES_PER_LOOP));
                    } catch (Throwable e) {
                        buffered.release(MAX_MESSAGES_PER_LOOP);
                        Throwables.propagate(e);
                    }

                    if (result == null || result.getMessages() == null || result.getMessages().isEmpty()) {
                        buffered.release(MAX_MESSAGES_PER_LOOP);
                        continue;
                    }

                    for (Message message : result.getMessages()) {
                        final MessageAttributeValue type = message.getMessageAttributes() != null
                            ? message.getMessageAttributes().get(ATTRIBUTE_TYPE)
                            : null;

                        if (type == null) {
                            Try.run(() -> sqsClient.deleteMessage(new DeleteMessageRequest()
                                .withReceiptHandle(message.getReceiptHandle())));
                            continue;
                        }

                        final String actionType = Try.of(type::getStringValue).getOrElse("");

                        if (actionType == null || actionType.isEmpty()) {
                            Try.run(() -> sqsClient.deleteMessage(new DeleteMessageRequest()
                                .withReceiptHandle(message.getReceiptHandle())));
                            continue;
                        }

                        final WorkerActionProvider<?, ?> actionProvider = ActionManager.getWorkerAction(actionType);
                        if (actionProvider == null) {
                            Try.run(() -> sqsClient.deleteMessage(new DeleteMessageRequest()
                                .withReceiptHandle(message.getReceiptHandle())));
                            continue;
                        }

                        final String body = Strings.nullToEmpty(message.getBody());
                        final Object in = body.isEmpty() ? null : WireFormat.parse(actionProvider.getInClass(), body);
                    }
                } catch (InterruptedException e) {

                } catch (Throwable e) {

                }
            }
        }
    }

    private class ActionQueue {
        private final AtomicInteger running = new AtomicInteger(0);
        private WorkerActionProvider actionProvider;
        private int maxConcurrentRequests;
        private int executionMillis;
        private LinkedList<ActionRequest> backlog;

        public void add(final ActionRequest request) {
            synchronized (this) {
                if (maxConcurrentRequests > 0 && running.get() >= maxConcurrentRequests) {
                    backlog.add(request);
                    return;
                }

                running.incrementAndGet();
            }

            run(request);

            if (executionMillis > 15000) {

            }
        }

        private void run(ActionRequest request) {
            actionProvider.observe(request.in).subscribe(
                r -> {
                    try {
                        if (r == Boolean.TRUE) {
                            // Delete message.
                            deleteQueue.add(request.receiptHandle);
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
                        // Do not
                        buffered.release();
                    } finally {
                        running.decrementAndGet();
                    }
                }
            );
        }
    }

    private class ActionRequest {
        final String receiptHandle;
        final Object in;

        public ActionRequest(String receiptHandle, Object in) {
            this.receiptHandle = receiptHandle;
            this.in = in;
        }
    }
}
