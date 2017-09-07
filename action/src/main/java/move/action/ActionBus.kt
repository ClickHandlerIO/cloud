package move.action

import com.google.common.util.concurrent.AbstractExecutionThreadService
import com.google.common.util.concurrent.AbstractIdleService
import io.nats.client.Message
import move.NUID
import net.openhft.chronicle.queue.ChronicleQueue
import net.openhft.chronicle.queue.ExcerptAppender
import net.openhft.chronicle.queue.ExcerptTailer
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder
import rx.Single

interface WithGroupId {
   val toGroupId: ByteArray
}

interface WithDeduplicationId {
   val toDeduplicationId: ByteArray
}





class ActionBus {
   fun publish(channel: String, queueName: String = "") {
   }

   fun ask(channel: String, messagesExpected: Int = 1): Single<Message> {
      return Single.just(Message())
   }
}




private val EMPTY_BYTE_ARRAY = ByteArray(0)

/**
 * Internal Worker Envelope.
 *
 * ACK-ACK
 * ACK
 */
data class ActionMessage(
   // UID of message.
   val id: String = NUID.nextGlobal(),
   // Group or Partition ID.
   val groupId: String? = null,
   // De-Duplication ID. Note that it only De-Duplicates within
   // the scope of the Group ID. Only valid when using a Job Pattern.
   val deduplicationId: String? = null,
   // Unix Nanosecond Epoch
   val created: Long = 0,
   // Earliest Unix Nanosecond Epoch to start Action.
   val delay: Long = 0,
   // Action name.
   val name: String = "",
   // Model type. null defaults to the Action's "Request" type
   val type: String? = null,
   // Unix Nanosecond epoch that is the target to
   // process this message by
   val target: Long = 0,
   // Unix Nanosecond epoch that acts as the deadline
   // for this message to be processed.
   val deadline: Int = 0,
   // Should this message be ignored if past
   // the deadline?
   val cancelAfterDeadline: Boolean = true,
   // Raw payload bytes
   val body: ByteArray = EMPTY_BYTE_ARRAY
)

/**
 *
 */
data class MoveMessageAck(
   // Move Internal ID
   val id: String,
   // If broker has it's own ID then it's set here.
   val brokerID: String? = null,
   // Node that is processing request.
   val nodeID: String,
   // Unix Nanosecond epoch of when remote
   // node will cancel request if not complete.
   val until: Long = 0
)

/**
 *
 */
data class MoveMessageNack(
   val reason: Int,
   val nodeID: String,
   val alternateChannel: String = ""
)


enum class WorkerType {
   /**
    * Broker nodes keep a persistent queue of all work
    * and also processes each message.
    */
   BROKER,
   /**
    * FLEX nodes offer unreliable processing power to
    * the network. BROKER nodes can use FLEX nodes to
    * "steal" work from them. Networks are rarely perfectly
    * balanced.
    */
   FLEX,
}

enum class QueueType {
   /**
    *
    */
   ACTOR,
   /**
    *
    */
   LOW,
   /**
    *
    */
   HIGH,
   /**
    * Low Priority Blocking Actions
    */
   BLOCKING_LOW,
   /**
    * High Priority Blocking Actions
    */
   BLOCKING_HIGH,
   /**
    *
    */
   LARGE_HIGH,
   /**
    *
    */
   LARGE_LOW,
}

/**
 *
 */
object ActionQueue {
   val queue = SingleChronicleQueueBuilder.binary("/Users/clay/move/trades").build()

   // There are 4
   var actors: PhysicalQueue? = null
   var high: PhysicalQueue? = null
   var low: PhysicalQueue? = null
   var blockingHigh: PhysicalQueue? = null
   var blockingLow: PhysicalQueue? = null

   @JvmStatic
   fun main(args: Array<String>) {
      println("Writing...")
      write()

      println("Reading...")
      read()

      queue.close()
   }

   fun write() {
      val appender = queue.acquireAppender()

      for (i in 1..5) {
         val start = System.currentTimeMillis()
         for (i in 1..1000000) {
            appender.writeDocument {
               it.writeEventName(i.toString())

            }
            appender.lastIndexAppended()
         }
         println(System.currentTimeMillis() - start)
      }
   }

   fun read() {
      val reader = queue.createTailer()

      reader.moveToIndex(0)

      for (i in 1..100) {
         val sb = StringBuilder()
         reader.readDocument {
            it.readEventName(sb)

            println(sb.toString())

            sb.setLength(0)
         }
      }
   }

   class PhysicalQueue(val queue: ChronicleQueue) : AbstractIdleService() {
      var appender: QAppender? = null
      var reader: QTailer? = null

      override fun startUp() {
         appender = QAppender()
         reader = QTailer()
      }

      override fun shutDown() {
         queue.close()
      }

      inner class QAppender(val appender: ExcerptAppender = queue.acquireAppender()) : AbstractExecutionThreadService() {
         override fun run() {
            queue.acquireAppender()
         }
      }

      inner class QTailer(val reader: ExcerptTailer = queue.createTailer()) : AbstractExecutionThreadService() {
         override fun run() {

         }
      }
   }
}
