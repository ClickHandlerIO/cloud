package move.action

/**

 */
class SQSQueueConfig {
   /**
    * Application queue name.
    */
   var name: String? = null
   /**
    *
    */
   var parallelism: Int = 0
   /**
    * The maximum number of entries in a batch command
    */
   var maxBatchSize = -1
   /**
    * The maximum time (milliseconds) a send batch is held open for additional outbound requests.
    * The longer this timeout, the longer messages wait for other messages to be added to the
    * batch. Increasing this timeout reduces the number of calls made and increases throughput, but
    * also increases average message latency.
    */
   var maxBatchOpenMs = -1
   /**
    * The maximum number of concurrent receive message batches. The greater this number, the faster
    * the queue will be pulling messages from the SQS servers (at the expense of consuming more
    * threads).
    */
   var maxInflightReceiveBatches = -1
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
   var maxDoneReceiveBatches = -1
}
