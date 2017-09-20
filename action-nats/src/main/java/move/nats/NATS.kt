package move.nats

import io.netty.buffer.ByteBuf
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetClientOptions
import io.vertx.core.net.NetSocket
import move.NUID
import move.action.MoveEventLoop
import move.action.MoveKernel
import move.common.WireFormat
import rx.Single
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

internal val EMPTY_BYTE_ARRAY = ByteArray(0)

/**
 * Attempts to maintain at least 1 connection per CPU core.
 * Connections will be evenly distributed across all NATS
 * servers in the cluster. Supports auto cluster
 * reconfiguration. Auto reconnects dropped connections and
 * has a backoff when dropped for being a slow consumer.
 *
 * @author Clay Molocznik
 */
class NATSCluster(val vertx: Vertx,
                  urls: List<Pair<String, Int>>,
                  var connectionsPerServer: Int = 2,
                  var connectionsPerCore: Int = 1) {

   val context = vertx.orCreateContext
   var running = true

   val eventLoopCounter = AtomicInteger(0)

   var connections: ArrayList<Connection> = ArrayList(16)
   var connecting: ArrayList<Connection> = ArrayList(16)

   var servers = urls.map { Server(this, it.first, it.second, 2) }.toList()

   var netClient = vertx.createNetClient(NetClientOptions()
      .setUsePooledBuffers(true)
      .setTcpNoDelay(true)
      .setReceiveBufferSize(NATSParser.RECV_BUFFER_SIZE)
      .setSendBufferSize(NATSParser.SEND_BUFFER_SIZE)
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
   fun nextEventLoop(): MoveEventLoop {
      val num = eventLoopCounter.getAndIncrement()
      if (eventLoopCounter.get() < 0)
         eventLoopCounter.set(0)

      return MoveKernel.eventLoops[num % MoveKernel.eventLoops.size]
   }

   fun pub(subject: ByteArray, replyTo: ByteArray = EMPTY_BYTE_ARRAY, buffer: ByteArray) {

   }

   /**
    * Creates a subscription across each server and each server's listener.
    */
   fun stripedSub() {

   }
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

class Server(val cluster: NATSCluster,
             val host: String,
             val port: Int,
             var poolSize: Int) {
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
                 val context: MoveEventLoop) : NATSParser.Listener {

   val counter = AtomicLong(0L)
   val subscriptions = mutableMapOf<Long, Sub>()
   val subscriptionsByClass = mutableMapOf<ByteArray, Long>()
   var stateTimestamp = System.nanoTime()
   var state = ConnectionState.DISCONNECTED
   var socket: NetSocket? = null
   var parser = NATSParser(this)

   var lastPingReceivedMs = 0L
   var lastPongReceivedMs = 0L

   val queue = ConcurrentLinkedQueue<Any>()
   var info: Info? = null

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
      parser = NATSParser(this)
      counter.set(0L)
      state = ConnectionState.DISCONNECTED
      socket = null
      server.connect()
   }

   override fun onSubject(byteBuf: ByteBuf?) {
      TODO("not implemented")
   }

   override fun onMessage(sid: Long, replyTo: ByteArray?, replyToLength: Int, payload: ByteBuf?) {
      TODO("not implemented")
   }

   override fun onInfo(buffer: ByteBuf) {
      info = WireFormat.parse(Info::class.java, buffer)
      if (info == null) {
         protocolErr("Malformed INFO payload: " + buffer.toString(StandardCharsets.UTF_8))
      }
   }

   fun processAsyncInfo(byteArray: ByteArray, offset: Int, length: Int) {
      // Cluster state changed.
   }

   override fun protocolErr(msg: String) {

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