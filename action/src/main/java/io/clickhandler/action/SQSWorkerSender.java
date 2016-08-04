package io.clickhandler.action;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;
import com.amazonaws.services.sqs.model.SendMessageBatchResultEntry;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.AbstractIdleService;
import javaslang.control.Try;
import rx.Observable;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

import static io.clickhandler.action.SQSWorkerReceiver.MAX_MESSAGES_PER_LOOP;

/**
 *
 */
public class SQSWorkerSender extends AbstractIdleService implements WorkerSender {
    private final LinkedBlockingDeque<WorkerRequest> queue = new LinkedBlockingDeque<>();

    private AmazonSQSClient sqsClient;
    private SendThread[] sendThreads;

    @Inject
    public SQSWorkerSender() {
    }

    public void setSqsClient(AmazonSQSClient sqsClient) {
        this.sqsClient = sqsClient;
    }

    @Override
    protected void startUp() throws Exception {
        sendThreads = new SendThread[Runtime.getRuntime().availableProcessors()];
        for (int i = 0; i < sendThreads.length; i++) {
            sendThreads[i] = new SendThread();
            sendThreads[i].startAsync().awaitRunning();
        }
    }

    @Override
    protected void shutDown() throws Exception {
        for (SendThread thread : sendThreads) {
            Try.run(() -> thread.stopAsync().awaitTerminated());
        }
    }

    private void send(HashMap<String, Entry> batch) {
        final List<SendMessageBatchRequestEntry> entries = batch.values().stream()
            .map($ -> $.entry).collect(Collectors.toList());

        final SendMessageBatchRequest request = new SendMessageBatchRequest()
            .withEntries(entries);

        try {
            final SendMessageBatchResult result = sqsClient.sendMessageBatch(request);

            final List<SendMessageBatchResultEntry> success = result.getSuccessful();
            if (success != null && !success.isEmpty()) {
                success.forEach(e -> {
                    final Entry entry = batch.get(e.getId());
                    if (entry != null)
                        entry.success = true;
                });
            }
        } catch (Throwable e) {
            // Ignore.
        }

        batch.values().forEach(entry -> {
            if (entry == null || entry.request == null || entry.request.subscriber == null)
                return;

            Try.run(() -> {
                entry.request.subscriber.onNext(entry.success);
                entry.request.subscriber.onCompleted();
            });
        });
    }

    @Override
    public Observable<Boolean> send(WorkerRequest request) {
        return Observable.create(subscriber -> {
            request.subscriber = subscriber;
            queue.add(request);
        });
    }

    private static final class Entry {
        final WorkerRequest request;
        final SendMessageBatchRequestEntry entry;
        boolean success = false;

        public Entry(WorkerRequest request, SendMessageBatchRequestEntry entry) {
            this.request = request;
            this.entry = entry;
        }
    }

    private class SendThread extends AbstractExecutionThreadService {
        @Override
        protected void run() throws Exception {
            while (isRunning()) {
                try {
                    final HashMap<String, Entry> batch = new HashMap<>(10);
                    WorkerRequest workerRequest;
                    while (true) {
                        workerRequest = queue.take();
                        if (workerRequest == null)
                            break;

                        final String id = Integer.toString(batch.size());
                        batch.put(id, new Entry(
                            workerRequest,
                            new SendMessageBatchRequestEntry()
                                .withId(id)
                                .withDelaySeconds(workerRequest.delaySeconds)
                                .withMessageBody(workerRequest.payload)
                        ));

                        if (batch.size() == MAX_MESSAGES_PER_LOOP) {
                            send(batch);
                            batch.clear();
                        }
                    }

                    if (!batch.isEmpty()) {
                        send(batch);
                    }
                } catch (Throwable e) {
                    // Ignore.
                }
            }
        }
    }
}
