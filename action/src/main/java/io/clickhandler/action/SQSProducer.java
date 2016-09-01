package io.clickhandler.action;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.*;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.AbstractIdleService;
import io.clickhandler.common.MicroMap;
import io.clickhandler.common.WireFormat;
import io.vertx.rxjava.core.Vertx;
import javaslang.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * AmazonSQS Worker Request producer.
 */
public class SQSProducer extends AbstractIdleService implements WorkerProducer {
    private static final Logger LOG = LoggerFactory.getLogger(SQSProducer.class);

    private final LinkedBlockingDeque<WorkerRequest> queue = new LinkedBlockingDeque<>();
    private final Vertx vertx;
    private AmazonSQS sqsClient;
    private SendThread[] sendThreads;
    private int batchSize;
    private int threadCount;
    private String queueUrl;
    private SQSWorkerConfig config;

    /**
     * @param vertx
     */
    SQSProducer(Vertx vertx) {
        this.vertx = vertx;
    }

    /**
     * @param sqsClient
     */
    void setSqsClient(AmazonSQS sqsClient) {
        this.sqsClient = sqsClient;
    }

    /**
     * @param queueUrl
     */
    void setQueueUrl(String queueUrl) {
        this.queueUrl = queueUrl;
    }

    /**
     * @param config
     */
    void setConfig(SQSWorkerConfig config) {
        Preconditions.checkArgument(config.batchSize > 0, "batchSize must be 1-10");
        Preconditions.checkArgument(config.batchSize <= 10, "batchSize must be 1-10");
        Preconditions.checkArgument(config.sendThreads > 0, "sendThreads must be greater than 0");
        this.config = config;
        this.batchSize = config.batchSize;
        this.threadCount = config.sendThreads;
    }

    /**
     * @param request
     * @return
     */
    @Override
    public Observable<Boolean> send(WorkerRequest request) {
        request.ctx = Vertx.currentContext();
        return Observable.create(subscriber -> {
            request.subscriber = subscriber;
            queue.add(request);
        });
    }

    @Override
    protected void startUp() throws Exception {
        Preconditions.checkNotNull(queueUrl, "queueUrl must be set");
        Preconditions.checkArgument(!queueUrl.isEmpty(), "queueUrl must be set");

        sendThreads = new SendThread[threadCount];
        for (int i = 0; i < sendThreads.length; i++) {
            sendThreads[i] = new SendThread();
            sendThreads[i].startAsync().awaitRunning();
        }
    }

    @Override
    protected void shutDown() throws Exception {
        for (SendThread thread : sendThreads) {
            Try.run(() -> thread.stopAsync().awaitTerminated(5, TimeUnit.SECONDS));
        }

        Try.run(() -> sqsClient.shutdown());
    }

    /**
     * @param batch
     */
    private void send(HashMap<String, Entry> batch) {
        final List<SendMessageBatchRequestEntry> entries = batch.values().stream()
            .map($ -> $.entry).collect(Collectors.toList());

        try {
            final SendMessageBatchResult result = doSend(
                new SendMessageBatchRequest()
                    .withQueueUrl(queueUrl)
                    .withEntries(entries));

            final List<SendMessageBatchResultEntry> success = result.getSuccessful();
            if (success != null && !success.isEmpty()) {
                success.forEach(e -> {
                    final Entry entry = batch.get(e.getId());
                    if (entry != null)
                        entry.success = true;
                });
            }
        } catch (SocketException e) {
            // Ignore.
        } catch (AmazonClientException e) {
            // Ignore.
        } catch (InterruptedException e) {
            // Ignore.
        } catch (Throwable e) {
            LOG.error("AmazonSQSClient.sendMessageBatch() threw an exception", e);
        }

        batch.values().forEach(entry -> {
            // Can we skip notifying with the result?
            if (entry == null || entry.request == null || entry.request.subscriber == null)
                return;

            Try.run(() -> {
                // Send notification on the Context.
                if (entry.request.ctx != null) {
                    entry.request.ctx.runOnContext(event -> {
                        entry.request.subscriber.onNext(entry.success);
                        entry.request.subscriber.onCompleted();
                    });
                } else {
                    vertx.runOnContext(event -> {
                        entry.request.subscriber.onNext(entry.success);
                        entry.request.subscriber.onCompleted();
                    });
                }
            });
        });
    }

    private SendMessageBatchResult doSend(SendMessageBatchRequest request)
        throws InterruptedException, SocketException {
        return sqsClient.sendMessageBatch(request);
    }

    private static final class Entry {
        final WorkerRequest request;
        final SendMessageBatchRequestEntry entry;
        boolean success = false;

        private Entry(WorkerRequest request, SendMessageBatchRequestEntry entry) {
            this.request = request;
            this.entry = entry;
        }
    }

    /**
     *
     */
    private class SendThread extends AbstractExecutionThreadService {
        private Thread thread;

        @Override
        protected String serviceName() {
            return "worker-producer-" + config.name + "-SQS-" + config.sqsName;
        }

        @Override
        protected void triggerShutdown() {
            Try.run(() -> thread.interrupt());
        }

        @Override
        protected void run() throws Exception {
            thread = Thread.currentThread();

            while (isRunning()) {
                try {
                    final HashMap<String, Entry> batch = new HashMap<>(batchSize);
                    final ArrayList<WorkerRequest> takeBatch = new ArrayList<>(batchSize);
                    while (true) {
                        int size = queue.drainTo(takeBatch, batchSize);

                        if (size == 0) {
                            final WorkerRequest workerRequest = queue.take();
                            if (workerRequest == null)
                                break;

                            takeBatch.add(workerRequest);
                            if (batchSize > 1)
                                queue.drainTo(takeBatch, batchSize - 1);
                        }

                        for (int i = 0; i < takeBatch.size(); i++) {
                            final WorkerRequest workerRequest = takeBatch.get(i);
                            final Map<String, MessageAttributeValue> attributes = new MicroMap<>(
                                SQSService.ATTRIBUTE_NAME,
                                new MessageAttributeValue()
                                    .withDataType("String")
                                    .withStringValue(workerRequest.actionProvider.getName())
                            );

                            final String id = Integer.toString(i);
                            batch.put(id, new Entry(
                                workerRequest,
                                new SendMessageBatchRequestEntry()
                                    .withId(id)
                                    .withMessageAttributes(attributes)
                                    .withDelaySeconds(
                                        workerRequest.delaySeconds > 0 ? workerRequest.delaySeconds : null)
                                    .withMessageBody(WireFormat.stringify(workerRequest.request))
                            ));
                        }

                        // Clear the takeBatch.
                        takeBatch.clear();

                        try {
                            // Send to SQS.
                            send(batch);
                        } finally {
                            // Clear the batch.
                            batch.clear();
                        }
                    }
                } catch (InterruptedException e) {
                    return;
                } catch (Throwable e) {
                    // Ignore.
                    LOG.error("Unexpected exception thrown in SendThread.run()", e);
                }
            }
        }
    }
}
