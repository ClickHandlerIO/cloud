package move.action

import com.sun.xml.internal.ws.message.stream.StreamMessage
import io.vertx.core.buffer.Buffer
import io.vertx.rxjava.core.Vertx
import move.NUID

class StreamConnector(val vertx: Vertx) {
   val eventLoopGroup = MoveThreadManager.get(vertx)
}

/**
 * Reactive Stream Action core. Streams are bi-directional multiplexed virtual raw TCP
 * Sockets. Stream Actions are a special type of Worker that doesn't have an explicit
 * deadline. Being built on top of WorkerActions which utilize the MoveApp Cloud Job Engine, complex
 * scenarios and synchronization is possible.
 *
 * The stream of data can be routed directly to another node and doesn't need to go
 * through Redis PubSub.
 *
 * Auto Failover is available and since all stream meta-data is kept in Redis Cluster, we
 * can enforce that "At-Most" is adhered to.
 *
 * Streams support backpressure.
 *
 * @author Clay Molocznik
 */
abstract class AbstractStreamAction<IN : StreamRequest<ARGS>, OUT : StreamReply, ARGS : Any> : InternalAction<IN, OUT>() {
//   suspend override fun execute(): OUT {
//      buildSequence<Buffer> {
//         yield(Buffer.buffer())
//      }.takeWhile { true }
//   }
}

enum class FailoverOption {
   CLOSE,
   RECOVER,
}

val STREAM_TYPE_PACKET = 1
val STREAM_TYPE_MESSAGE = 2

/**
 *
 */
data class StreamRequest<T>(
   //
   val id: String = NUID.nextGlobal(),
   //
   val groupKey: String = NUID.nextGlobal(),
   //
   val nodeAddress: String = "",
   //
   val nodePort: Int = 0,
   // PubSub channel to publish messages on
   val channel: String = "",
   // Local lambda
   val local: suspend (StreamMessage) -> Unit,
   // Try to compress payload
   val gzip: Boolean = true,
   // Args for setup
   val args: T? = null
)

object StreamClose

data class StreamCloseChannel(val channel: Long)


/**
 *
 */
data class StreamFrame(
   // Global sequence
   val sequence: Long,
   // Logical channel (i.e. Multiplexed TCP / HTTP)
   val channel: Long,
   // Frame sequence
   val frame: Long,
   // User defined type
   val type: Int,
   // Payload of raw bytes
   val payload: Buffer
)

val STREAM_REPLY_STARTED = 1
val STREAM_REPLY_STOPPED = 2
val STREAM_REPLY_FAILED = 3
val STREAM_REPLY_LOST_NODE_CONNECTION = 4

/**
 *
 */
data class StreamReply(
   //
   val closed: Boolean = false,
   val code: Int
)