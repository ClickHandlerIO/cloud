package move.action;

import com.amazonaws.services.sqs.buffered.SendQueueBuffer;

import java.util.ArrayList;
import java.util.Set;

/**
 *
 */
public class SQSConfig {
    public String namespace = "dev";

    /**
     *
     */
    public String region;

    /**
     *
     */
    public String awsAccessKey;

    /**
     *
     */
    public String awsSecretKey;

    /**
     *
     */
    public int maxThreads = 1024;

    /**
     * The maximum number of entries in a batch command
     */
    public int maxBatchSize = 10;

    /**
     * The maximum time (milliseconds) a send batch is held open for additional outbound requests.
     * The longer this timeout, the longer messages wait for other messages to be added to the
     * batch. Increasing this timeout reduces the number of calls made and increases throughput, but
     * also increases average message latency.
     */
    public int maxBatchOpenMs = 100;

    /**
     * The maximum number of concurrent receive message batches. The greater this number, the faster
     * the queue will be pulling messages from the SQS servers (at the expense of consuming more
     * threads).
     */
    public int maxInflightReceiveBatches = 0;

    /**
     * If more than that number of completed receive batches are waiting in the buffer, the querying
     * for new messages will stop. The larger this number, the more messages the buffer queue will
     * pre-fetch and keep in the buffer on the client side, and the faster receive requests will be
     * satisfied. The visibility timeout of a pre-fetched message starts at the point of pre-fetch,
     * which means that while the message is in the local buffer it is unavailable for other clients
     * to process, and when this client retrieves it, part of the visibility timeout may have
     * already expired. The number of messages prefetched will not exceed maxBatchSize *
     * maxDoneReceiveBatches.
     */
    public int maxDoneReceiveBatches = 0;

    /**
     * Option to configure flushOnShutdown. Enabling this option will flush the pending requests in the
     * {@link SendQueueBuffer} during shutdown.
     */
    public boolean flushOnShutdown = true;

    /**
     *
     */
    public boolean worker;

    /**
     *
     */
    public Set<String> inclusions;
    /**
     *
     */
    public Set<String> exclusions;

    /**
     *
     */
    public ArrayList<SQSQueueConfig> queues;
}
