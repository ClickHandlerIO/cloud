package move.nats

import io.netty.buffer.ByteBuf
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetClientOptions
import io.vertx.core.net.NetSocket
import move.NUID
import move.action.ActionEventLoopContext
import move.action.ActionEventLoopGroup
import rx.Single
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

internal val EMPTY_BYTE_ARRAY = ByteArray(0)

class NATSCluster(val vertx: Vertx,
                  urls: List<Pair<String, Int>>,
                  var connectionsPerServer: Int = 2) {

   val context = vertx.orCreateContext
   var running = true

   val eventLoopGroup = ActionEventLoopGroup.get(vertx)
   val eventLoopCounter = AtomicInteger(0)

   var connections: ArrayList<Connection> = ArrayList(16)
   var connecting: ArrayList<Connection> = ArrayList(16)

   var servers = urls.map { Server(this, it.first, it.second, 2) }.toList()

   var netClient = vertx.createNetClient(NetClientOptions()
      .setUsePooledBuffers(true)
      .setTcpNoDelay(true)
      .setReceiveBufferSize(RECV_BUFFER_SIZE)
      .setSendBufferSize(SEND_BUFFER_SIZE)
   )

   fun start() {
      context.runOnContext {
         servers.forEach { it.connect() }
      }
   }

   fun ensureConnections() {
      context.runOnContext {
         servers.forEach { it.connect() }
      }
   }

   enum class State {
      STARTING,
      RECONFIGURING,
      RUNNING,
   }

   /**
    * Spread out our I/O across the CPU cores.
    */
   @Synchronized
   fun nextEventLoop(): ActionEventLoopContext {
      val num = eventLoopCounter.getAndIncrement()
      if (eventLoopCounter.get() < 0)
         eventLoopCounter.set(0)

      return eventLoopGroup.executors[num % eventLoopGroup.executors.size]
   }

   fun pub(subject: ByteArray, replyTo: ByteArray = EMPTY_BYTE_ARRAY, buffer: ByteArray) {

   }

   /**
    * Creates a subscription across each server and each server's connection.
    */
   fun stripedSub() {

   }
}

internal val RECV_BUFFER_SIZE = 2 * 1024 * 1024
internal val SEND_BUFFER_SIZE = 2 * 1024 * 1024

internal val ascii_0 = 48
internal val ascii_9 = 57

internal val ZERO = '0'.toByte()
internal val ONE = '1'.toByte()
internal val TWO = '2'.toByte()
internal val THREE = '3'.toByte()
internal val FOUR = '4'.toByte()
internal val FIVE = '5'.toByte()
internal val SIX = '6'.toByte()
internal val SEVEN = '7'.toByte()
internal val EIGHT = '8'.toByte()
internal val NINE = '9'.toByte()

val digits = arrayOf(ZERO, ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE)

class SimpleParser {


   fun parse(buffer: ByteBuf) {

   }
}

enum class ParserState {
   M_OR_P_OR_I_PLUS_OR_MINUS,
}



fun Buffer.appendNumberAsText(msgSize: Int) {
   if (msgSize > 0) {
      var l = msgSize
      while (l > 0) {
         appendByte(digits[l % 10])
         l /= 10
      }
   } else {
      appendByte(digits[0])
   }
}

fun Buffer.readAsciiInt64(length: Int): Long {
   var num:Long = 0

   var dec:Byte
   for (i in 0..length) {
      dec = getByte(i)
      if (dec < ascii_0 || dec > ascii_9) {
         return -1
      }
      num = (num * 10) + dec - ascii_0
   }
   return num
}

enum class ConnectionState {
   DISCONNECTED,
   CONNECTING,
   CONNECTION_REJECTED,
   WAITING_FOR_INFO,
   INFO_NOT_RECEIVED,
   SENT_CONNECT,
   ESTABLISHED,
}

class Server(val cluster: NATSCluster, val host: String, val port: Int, var poolSize: Int) {
   val connections = Array<Connection>(poolSize) {
      Connection(cluster, this, cluster.nextEventLoop())
   }

   fun connectionFailed(connection: Connection, exception: Throwable) {

   }

   fun connect() {
      if (!cluster.running)
         return

      connections.forEach { it.connect() }
   }
}

data class Info(val server_id: String = "",
                val version: String = "",
                val go: String = "",
                val host: String = "",
                val port: Int = 0,
                val auth_required: Boolean = false,
                val ssl_required: Boolean = false,
                val tls_required: Boolean = false,
                val tls_verify: Boolean = false,
                val max_payload: Int = 0,
                val connect_urls: List<String> = listOf())

data class Connect(val verbose: Boolean = false,
                   val pedantic: Boolean = false,
                   val ssl_required: Boolean = false,
                   val auth_token: String? = null,
                   val user: String? = null,
                   val pass: String? = null,
                   val name: String? = null,
                   val lang: String = "kotlin",
                   val version: String = "0.1.0",
                   val protocol: Int = 1)

data class Pub(val subject: String,
               val replyTo: String? = null,
               val payload: Buffer)

data class RequestReply(val stripedId: Long,
                        val replyTo: String = NUID.nextGlobal(),
                        val payload: ByteArray)

data class RequestReplyJson(val stripedId: Long,
                            val replyTo: String = NUID.nextGlobal(),
                            val payload: Any) {
   var single: Single<Any>? = null
}

data class RequestReplyMsgPack(val stripedId: Long,
                               val replyTo: String = NUID.nextGlobal(),
                               val payload: Any) {
   var single: Single<Any>? = null
}

data class Sub(val subject: String,
               val queue_group: String,
               val sid: String) {

   val listeners = ArrayList<Any>(1)
}

data class Unsub(val sid: String,
                 val max_msgs: Int = 0)

data class Msg(val subject: String,
               val sid: String,
               val replyTo: String? = null,
               val payload: Buffer)

object Ping

object Pong

val PING = "PING\r\n".toByteArray()
val PONG = "PONG\r\n".toByteArray()

/**
 *
 */
class Connection(val cluster: NATSCluster,
                 val server: Server,
                 val context: ActionEventLoopContext) {

   val counter = AtomicLong(0L)
   val subscriptions = mutableMapOf<ByteArray, Sub>()
   var stateTimestamp = System.nanoTime()
   var state = ConnectionState.DISCONNECTED
   var socket: NetSocket? = null
   var parser = Parser(this)

   var lastPingReceivedMs = 0L
   var lastPongReceivedMs = 0L

   val queue = ConcurrentLinkedQueue<Any>()

   init {
   }

   fun sub() {
      val id = counter.incrementAndGet()

   }

   internal fun connect() {
      context.runOnContext {
         state = ConnectionState.CONNECTING

         cluster.netClient.connect(server.port, server.host) {
            if (it.failed()) {
               state = ConnectionState.DISCONNECTED
               stateTimestamp = System.nanoTime()
               server.connect()
            } else {
               state = ConnectionState.WAITING_FOR_INFO
               stateTimestamp = System.nanoTime()
            }
         }
      }
   }

   private fun handle(buffer: Buffer) {
      parser.parse(buffer.byteBuf)
   }

   private fun handle(exception: Throwable) {
      socket?.close()
   }

   private fun handleClose(void: Void) {
      parser = Parser(this)
      counter.set(0L)
      state = ConnectionState.DISCONNECTED
      socket = null
      server.connect()
   }

   fun processOk() {

   }

   fun processMsg(byteArray: ByteArray, offset: Int, length: Int) {
      parser.ps.ma.sid
   }

   fun processAsyncInfo(byteArray: ByteArray, offset: Int, length: Int) {
      // Cluster state changed.
   }

   fun protocolErr(msg: String) {

   }

   fun processErr(buffer: ByteBuffer) {

   }

   fun processPing() {
      lastPingReceivedMs = System.currentTimeMillis()

      // Write PONG.
      socket?.write(Buffer.buffer(PONG))
   }

   fun processPong() {
      lastPongReceivedMs = System.currentTimeMillis()
   }
}


object NATS {
   @JvmStatic
   fun main(args: Array<String>) {
//      val cluster = NATSCluster(
//         Vertx.vertx(),
//         listOf(Pair("localhost", 4222)),
//         1,
//         16
//      )
//      cluster.start()
//
//
//      Thread.sleep(100000000)
   }
}