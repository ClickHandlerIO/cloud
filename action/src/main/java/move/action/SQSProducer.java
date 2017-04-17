package move.action;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;
import com.amazonaws.services.sqs.model.SendMessageBatchResultEntry;
import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.AbstractIdleService;
import io.vertx.rxjava.core.Vertx;
import javaslang.control.Try;
import move.common.WireFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * AmazonSQS Worker Request producer.
 *
 * @author Clay Molocznik
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

    private Counter jobCounter;
    private Counter successCounter;
    private Counter failuresCounter;

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
        Preconditions.checkArgument(config.sendThreads > 0, "sendThreads must be greater than 0");
        this.config = config;
        this.threadCount = config.sendThreads;
        this.batchSize = 10; // Producer should always be 10.
    }

    /**
     * @param request
     * @return
     */
    @Override
    public Observable<Boolean> send(WorkerRequest request) {
        request.ctx = Vertx.currentContext();
        return Observable.unsafeCreate(subscriber -> {
            request.subscriber = subscriber;
            queue.add(request);
        });
    }

    @Override
    protected void startUp() throws Exception {
        Preconditions.checkNotNull(queueUrl, "queueUrl must be set");
        Preconditions.checkArgument(!queueUrl.isEmpty(), "queueUrl must be set");

        final MetricRegistry registry = SharedMetricRegistries.getOrCreate("app");
        jobCounter = registry.counter(config.name + "-PRODUCED");
        successCounter = registry.counter(config.name + "-SUCCESS");
        failuresCounter = registry.counter(config.name + "-FAILURE");

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
                successCounter.inc(success.size());
                success.forEach(e -> {
                    final Entry entry = batch.get(e.getId());
                    if (entry != null)
                        entry.success = true;
                });
            }

            if (result.getFailed() != null && !result.getFailed().isEmpty()) {
                failuresCounter.inc(result.getFailed().size());
            }
        } catch (SocketException e) {
            LOG.error("AmazonSQSClient.sendMessageBatch() threw a socket exception", e);
        } catch (AmazonClientException e) {
            LOG.error("AmazonSQSClient.sendMessageBatch() threw a client exception", e);
        } catch (InterruptedException e) {
            LOG.warn("AmazonSQSClient.sendMessageBatch() threw an interrupted exception", e);
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

                            final String id = Integer.toString(i);

                            if (config.fifo) {
                                String groupId = config.name;

                                if (!workerRequest.actionProvider.getMessageGroupId().isEmpty())
                                    groupId = workerRequest.actionProvider.getMessageGroupId();

                                if (workerRequest.groupId != null && !workerRequest.groupId.isEmpty())
                                    groupId = workerRequest.groupId;

                                batch.put(id, new Entry(
                                    workerRequest,
                                    new SendMessageBatchRequestEntry()
                                        .withId(id)
                                        .withMessageGroupId(groupId)
                                        .withMessageBody(WireFormat.stringify(workerRequest.request))
                                ));
                            } else {
                                batch.put(id, new Entry(
                                    workerRequest,
                                    new SendMessageBatchRequestEntry()
                                        .withId(id)
                                        .withDelaySeconds(
                                            workerRequest.delaySeconds > 0 ? workerRequest.delaySeconds : null)
                                        .withMessageBody(WireFormat.stringify(workerRequest.request))
                                ));
                            }
                        }

                        // Clear the takeBatch.
                        takeBatch.clear();

                        try {
                            jobCounter.inc(batch.size());

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
