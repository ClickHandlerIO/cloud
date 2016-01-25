package io.clickhandler.queue;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.model.*;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Queue Backed by AmazonSQS.
 *
 * @author Clay Molocznik
 */
public class AmazonSQSService<T> extends AbstractBackingQueueService<T> {
    private final int visibilityTimeoutSeconds;
    private final int pollDurationSeconds;
    private AWSCredentials credentials;
    // AmazonSQS Queue URL.
    private String url;
    // AmazonSQS Client.
    private AmazonSQSBufferedAsyncClient client;

    public AmazonSQSService(QueueServiceConfig<T> config) {
        super(config);

        this.visibilityTimeoutSeconds = 30;
        this.pollDurationSeconds = 20;
    }

    @Override
    protected void startUp() throws Exception {
        final String awsAccessKey = ""; // TODO: Fix
        final String awsSecretKey = "";

        if (awsAccessKey.isEmpty() || awsSecretKey.isEmpty()) {
            throw new RuntimeException("Invalid AWS Credentials");
        }

        this.credentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
        this.client = new AmazonSQSBufferedAsyncClient(new AmazonSQSAsyncClient(credentials));

        // Create Queue if needed and get the Queue URL.
        try {
            final GetQueueUrlResult result = client.getQueueUrl(name);
            this.url = Strings.nullToEmpty(result.getQueueUrl()).trim();
        } catch (QueueDoesNotExistException e) {
            final CreateQueueResult result = client.createQueue(name);
            this.url = Strings.nullToEmpty(result.getQueueUrl()).trim();
        }
    }

    @Override
    protected void shutDown() throws Exception {
        // Delete queue if necessary.
        if (config.isEphemeral()) {
            try {
                client.deleteQueue(url);
            } catch (Exception e) {
                log.error("Failed to delete Queue[" + name + "] at URL[" + url + "]");
            }
        }
        client.shutdown();
    }

    @Override
    public void add(T message) {
        Preconditions.checkNotNull(message, "message was null");
        final SendMessageRequest sendRequest = new SendMessageRequest(url, stringify(message));
        final SendMessageResult result = client.sendMessage(sendRequest);
    }

    @Override
    public void addAll(Collection<T> collection) {
        Preconditions.checkNotNull(collection, "collection was null");

        // Send in batches of "maxMessages"
        List<SendMessageBatchRequestEntry> entries = Lists.newArrayListWithExpectedSize(collection.size() < maxMessages ? collection.size() : maxMessages);
        for (T message : collection) {
            final SendMessageBatchRequestEntry entry = new SendMessageBatchRequestEntry(UUID.randomUUID().toString().replace("-", ""), stringify(message));
            entries.add(entry);
            if (entries.size() == maxMessages) {
                SendMessageBatchResult result = client.sendMessageBatch(new SendMessageBatchRequest(url, entries));
                entries = Lists.newArrayListWithExpectedSize(maxMessages);
            }
        }
    }

    protected void doRun() {
        final ReceiveMessageRequest request = new ReceiveMessageRequest(url);
        request.setMaxNumberOfMessages(maxMessages);
        request.setVisibilityTimeout(visibilityTimeoutSeconds);
        request.setWaitTimeSeconds(pollDurationSeconds);

        final ReceiveMessageResult result = client.receiveMessage(request);
        final List<Message> messages = result.getMessages();

        if (messages == null || messages.isEmpty()) {
            return;
        }

        final DeleteMessageBatchRequest deleteBatchRequest = new DeleteMessageBatchRequest(url);
        final List<DeleteMessageBatchRequestEntry> deleteRequests = Lists.newArrayListWithExpectedSize(messages.size());
        final List<T> parsedMessages = Lists.newArrayListWithCapacity(messages.size());

        for (int i = 0; i < messages.size(); i++) {
            final Message message = messages.get(i);
            try {
                deleteRequests.add(new DeleteMessageBatchRequestEntry(message.getMessageId(), message.getReceiptHandle()));

                final T parsed = parse(type, message.getBody());
                if (parsed != null) {
                    parsedMessages.add(parsed);
                } else {
                    log.warn("WireFormat.parse() returned null for incoming message with payload '" + message.getBody() + "'. Deleting message.");
                }
            } catch (Exception e) {
                log.error("Failed executing WireFormat.parse() for incoming message with payload '" + message.getBody() + "'", e);
            }
        }

        try {
            handler.receive(parsedMessages);
        } catch (Exception e) {
            log.error("Failed executing receive()", e);
        }

        if (!deleteRequests.isEmpty()) {
            deleteBatchRequest.setEntries(deleteRequests);
            try {
                final DeleteMessageBatchResult deleteResult = client.deleteMessageBatch(deleteBatchRequest);
//                    for (BatchResultErrorEntry errorEntry : deleteResult.getFailed()) {
//                    }
            } catch (Exception e) {
                log.error("Failed executing SQS.deleteMessageBatch", e);
            }
        }
    }

    public String stringify(Object obj) {
        return "";
    }

    public <T> T parse(Class<T> cls, String data) {
        return null;
    }
}
