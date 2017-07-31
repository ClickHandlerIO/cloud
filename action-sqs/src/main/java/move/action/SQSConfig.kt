package move.action

import com.amazonaws.services.sqs.buffered.SendQueueBuffer
import move.common.WireFormat
import java.util.*

/**

 */
class SQSConfig {
   var enabled: Boolean? = false

   /**
    *
    */
   var worker: Boolean? = true

   /**
    *
    */
   var namespace:String? = "dev"

   /**

    */
   var region: String? = null

   /**

    */
   var awsAccessKey: String? = null

   /**

    */
   var awsSecretKey: String? = null

   /**
    *
    */
   var s3AccessKey: String? = null
   /**
    *
    */
   var s3SecretKey: String? = null

   /**
    *
    */
   var s3BucketName: String? = null

   /**
    *
    */
   var maxPayloadSize = 1024 * 1024 * 10

   /**
    * The maximum number of entries in a batch command
    */
   var maxBatchSize = 10

   /**
    * The maximum time (milliseconds) a send batch is held open for additional outbound requests.
    * The longer this timeout, the longer messages wait for other messages to be added to the
    * batch. Increasing this timeout reduces the number of calls made and increases throughput, but
    * also increases average message latency.
    */
   var maxBatchOpenMs = 100

   /**
    * The maximum number of concurrent receive message batches. The greater this number, the faster
    * the queue will be pulling messages from the SQS servers (at the expense of consuming more
    * threads).
    */
   var maxInflightReceiveBatches = 1

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
   var maxDoneReceiveBatches = 1

   /**
    * Option to configure flushOnShutdown. Enabling this option will flush the pending requests in the
    * [SendQueueBuffer] during shutdown.
    */
   var flushOnShutdown = true

   /**
    *
    */
   var inclusions: Set<String>? = null
   /**
    *
    */
   var exclusions: Set<String>? = null

   /**
    *
    */
   var queues: ArrayList<SQSQueueConfig>? = null

   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         println(WireFormat.stringify(SQSConfig().apply {
            region = "us-west-2"
            awsAccessKey = ""
            awsSecretKey = ""
            s3AccessKey = ""
            s3SecretKey = ""
            s3BucketName = "sqs"
         }))
      }
   }
}
