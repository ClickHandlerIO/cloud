package move.action

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetServer
import io.vertx.core.net.NetServerOptions
import io.vertx.core.net.NetSocket
import org.lmdbjava.Dbi
import org.lmdbjava.DbiFlags
import org.lmdbjava.Env
import org.lmdbjava.Txn
import java.io.File
import java.nio.ByteBuffer


data class MNodeInfo(
   val globalId: Long,
   val created: Long,
   val role: NodeRole,
   val clusterId: Long,
   val host: String,
   val port: Int,
   val cpus: Int,
   val cores: Array<Core>) {

   data class Core(
      val globalId: Long,
      val id: Int,
      val host: String,
      val port: Int
   )
}

/**
 * Backing service to maintain cluster state.
 */
interface MClusterStore {
   suspend fun allNodes(): List<MNodeInfo>

   suspend fun remoteNodes(): List<MNodeInfo>

   suspend fun workerNodes(): List<MNodeInfo>

   suspend fun node(globalId: Long): MNodeInfo

   suspend fun locateActor(name: ActionName, proposedCore: Long): Long

   suspend fun actorStarting(name: ActionName, proposedCore: Long): Long

   suspend fun actorStopped(name: ActionName, proposedCore: Long): Long

   fun consumer(name: ActionName): MConsumer

   fun coreConsumer(globalId: Long): MConsumerRemote
}


/**
 * Internal stateful singleton containing currently active Cluster.
 */
object MCluster {
   val consumers: Map<ActionName, MConsumer> = mutableMapOf()

   val nodes: Map<Int, MClusterNode> = mutableMapOf()

   val cores: Map<Int, MClusterNode.Core> = mutableMapOf()

   val actors: Map<Int, MClusterActor> = mutableMapOf()

   val actions: Map<Int, MClusterAction> = mutableMapOf()
}

/**
 * Type of Actor
 */
class MClusterActor(val uid: Int, val name: ActionName) {
   /**
    * Remote Cores that will process requests.
    */
   val remoteCores: Map<Int, MClusterNode.Core> = mutableMapOf()

   /**
    * Local Cores that will process requests.
    */
   val localCores: Map<Int, MClusterNode.Core> = mutableMapOf()
}

/**
 *
 */
class MClusterAction(val uid: Int, val name: ActionName) {
   /**
    * Remote Cores that will process requests.
    */
   val remoteCores: Map<Int, MClusterNode.Core> = mutableMapOf()

   /**
    * Local Cores that will process requests.
    */
   val localCores: Map<Int, MClusterNode.Core> = mutableMapOf()
}

/**
 * A single node in the cluster.
 *
 * Each node has 1 or more Cores.
 * Cores map 1-to-1 with the MEventLoop.
 *
 * Each Core has exactly 1 connection to every other core.
 */
open class MClusterNode(
   val uid: Int,
   val created: Long,
   val role: NodeRole,
   val host: String,
   val port: Int,
   val cpus: Int,
   val workers: Int) {

   val cores: List<MClusterNode.Core> = arrayListOf()

   /**
    *
    */
   inner class Core(
      val uid: Int,
      val host: String,
      val port: Int) {

      var replicas: List<Core> = arrayListOf()
   }
}


/**
 * Local NetServer for an EventLoop.
 */
class MConsumerLocal(val eventLoop: MEventLoop) {

   val server = Server()
   val connections: ArrayList<Connection> = arrayListOf()
   var clusterCore: MClusterNode.Core? = null
   var siblings = arrayListOf<MConsumerLocal>()

   suspend fun startServer(options: NetServerOptions) {
      server.start(options)
   }

   /**
    * Server for Remote Cores to connect to this core.
    */
   inner class Server {
      lateinit var server: NetServer
      var running: Boolean = false
         get
         private set

      suspend fun start(options: NetServerOptions) {
         server = VERTX.delegate.createNetServer(options)

         server.connectHandler {
            //            connections.add(Connection(it))
         }

         server = awaitResult { event -> server.listen { event.handle(it) } }
         running = true
      }
   }

   inner class HandshakeConnection(val socket: NetSocket) {
      init {
         socket.closeHandler { }
         socket.handler {
            val remoteCore = it.byteBuf.readInt()

            if (MCluster.cores.containsKey(remoteCore)) {

            }
         }
         socket.exceptionHandler { }
      }
   }

   /**
    *
    */
   inner class Connection(val socket: NetSocket, val remoteCore: MClusterNode.Core) {

   }
}


/**
 * Connection to a Remote Core.
 */
class MConsumerRemote(val globalId: Long,
                      val nodeId: Int,
                      val host: String,
                      val port: Int) {
   var connection: Connection? = null

   init {
   }

   suspend fun connect() {
      connection = Connection(awaitResult<NetSocket> { result ->
         INTERNAL_NET_CLIENT.connect(port, host) {
            result.handle(it)
         }
      })
   }

   suspend fun send(buf: ByteBuf) {
      // Pass ByteBuf through.
      connection?.socket?.end(Buffer.buffer(buf))
   }

   /**
    *
    */
   inner class Connection(val socket: NetSocket) {
      init {
         socket.exceptionHandler { }
         socket.handler { }
         socket.closeHandler { }
      }
   }
}

/**
 *
 */
class MLocalConsumer(val eventLoop: MEventLoop) {
   // NetServer to listen for remote requests / events.
   var server: NetServer? = null
   val connections: ArrayList<Connection> = arrayListOf()
   val connectionsByGlobalId: MutableMap<Long, Connection> = mutableMapOf()
   val parser: InternalParser = InternalParser()

   fun handle(producer: ActionProducer<*, *, *, *>) {

   }

   suspend fun start() {
//      server = awaitResult<NetServer> { event -> server?.listen { event.handle(it) } }
   }

   inner class Connection(val socket: NetSocket) {
      val created = System.currentTimeMillis()
      var globalId = -1L
      var lastSequence = -1L

      init {
         socket.closeHandler {
            connections.remove(this)
            connectionsByGlobalId.remove(globalId)
         }

         socket.exceptionHandler { }

         socket.handler {
            if (globalId == -1L) {
               globalId = it.byteBuf.readLong()

               handle(it.byteBuf)

               socket.handler { handle(it.byteBuf) }
            } else {
               handle(it.byteBuf)
            }
         }
      }

      fun handle(buffer: ByteBuf) {
         // Handle protocol.
         parser.process(buffer)
      }
   }
}


/**
 *
 */
class MConsumer(val name: ActionName) {
   val local: List<MLocalConsumer> = arrayListOf()
   val remote: List<MConsumerRemote> = arrayListOf()

   fun pickLocal(): MLocalConsumer? {
      return null
   }

   fun pickRemote(): MConsumerRemote? {
      return null
   }
}


private const val REQUEST_ID_SIZE = 16

data class RequestID(val coreId: Long, val sequence: Long) {
   fun pack(buf: ByteBuf) {
      buf.ensureWritable(REQUEST_ID_SIZE)
      buf.writeLong(coreId)
      buf.writeLong(sequence)
   }
}

typealias StreamID = RequestID


internal const val INTERNAL_ASK: Byte = 1 // Expect Reply
internal const val INTERNAL_ASK_ACK: Byte = 2 // Expect ACK and forget Reply
internal const val INTERNAL_ASK_ACK_REPLY: Byte = 3 // Expect ACK and Reply
internal const val INTERNAL_ASK_PUSH: Byte = 4 // Fire and Forget
internal const val INTERNAL_PUSH_ASK: Byte = 5 // Push message while Action is executing

internal const val INTERNAL_ACTOR_ASK: Byte = 6 // Expect Reply
internal const val INTERNAL_ACTOR_ASK_ACK: Byte = 7 // Expect ACK and forget Reply
internal const val INTERNAL_ACTOR_ASK_ACK_REPLY: Byte = 8 // Expect ACK and Reply
internal const val INTERNAL_ACTOR_ASK_PUSH: Byte = 9 // Fire and Forget

internal const val INTERNAL_ACTOR_CHANNEL_FULL: Byte = 40

internal const val INTERNAL_ASK_TIMEOUT: Byte = 90 // Push message while Action is executing
internal const val INTERNAL_ASK_ERROR: Byte = 91 // Push message while Action is executing
internal const val INTERNAL_ASK_REDIRECT: Byte = 92 // Push message while Action is executing
internal const val INTERNAL_ASK_ACTOR_REDIRECT: Byte = 93 // Push message while Action is executing


internal const val INTERNAL_TOO_BUSY = 100


const val ACTOR_OPTION_ASYNC = 1
const val ACTOR_OPTION_SYNC = 2

interface InternalParserHandler {
   fun handleAsk(
      type: Byte,
      requestId: ActionName,
      name: ActionName,
      timeoutSeconds: Int,
      delaySeconds: Int,
      payload: ByteBuf
   )

   fun handleActorAsk(
      type: Byte,
      requestId: ActionName,
      actorName: ActionName,
      option: Int,
      name: ActionName,
      timeoutSeconds: Int,
      delaySeconds: Int,
      payload: ByteBuf
   )

   fun handleActorMessage(
      type: Byte,
      actorName: ActionName,
      messageType: ActionName,
      payload: ByteBuf
   )

   fun handleStream(
      type: Byte,
      streamId: ActionName,
      actorName: ActionName,
      payload: ByteBuf
   )
}

/**
 * Actor Ask
 * [TYPE][NameLength][ActorName][ActorOption][NameLength][ActionName][TimeoutSeconds][DelaySeconds][Compression][PayloadSize][Payload]
 *
 * Action Ask
 * [TYPE][NameLength][ActionName][TimeoutSeconds][DelaySeconds][Compression][PayloadSize][Payload]
 */
class InternalParser {
   var state = PARSER_TYPE
   var messageType: Byte = 0
   var typeSizeFirst: Byte = 0
   var typeSize: Short = 0
   var timeoutSeconds: Int = 0
   var lastBuffer: ByteBuf = Unpooled.EMPTY_BUFFER
   var payloadSize: Int = 0
   var payloadCompression: Byte = 0

   val typeSizeBuffer = Unpooled.buffer(2, 2)
   val requestIdBuffer = Unpooled.buffer(8, 8)

   fun process(buf: ByteBuf) {
      var b: Byte
      while (buf.isReadable) {
         b = buf.readByte()
         when (state) {
            PARSER_TYPE -> {
               messageType = b

               when (messageType) {
                  INTERNAL_ASK,
                  INTERNAL_ASK_ACK,
                  INTERNAL_ASK_ACK_REPLY,
                  INTERNAL_PUSH_ASK -> {
                     state = PARSER_ASK_REQUEST_ID
                  }

                  INTERNAL_ACTOR_ASK,
                  INTERNAL_ACTOR_ASK_ACK,
                  INTERNAL_ACTOR_ASK_ACK_REPLY,
                  INTERNAL_ACTOR_ASK_PUSH -> {
                     state = PARSER_ACTOR_ASK_REQUEST_ID
                  }
               }
            }

            PARSER_ASK_REQUEST_ID -> {
               if (buf.readableBytes() < requestIdBuffer.writableBytes()) {
                  buf.writeBytes(requestIdBuffer, buf.readableBytes())
               } else {
                  buf.writeBytes(requestIdBuffer, requestIdBuffer.writableBytes())
                  state = PARSER_ACTION_NAME_SIZE
               }
            }

            PARSER_ACTOR_ASK_REQUEST_ID -> {
               if (buf.readableBytes() < requestIdBuffer.writableBytes()) {
                  buf.writeBytes(requestIdBuffer, buf.readableBytes())
               } else {
                  buf.writeBytes(requestIdBuffer, requestIdBuffer.writableBytes())
                  state = PARSER_ACTOR_NAME_SIZE
               }
            }

            PARSER_ACTION_NAME_SIZE -> {
               if (!buf.isReadable) {
                  typeSizeFirst = b
                  state = PARSER_ACTION_NAME_SIZE_FINISH
               } else {
                  typeSize = buf.readShort()
                  state = PARSER_ACTION_NAME
               }
            }

            PARSER_ACTION_NAME_SIZE_FINISH -> {

            }
         }
      }
   }
}

private const val COMPRESSION_NONE: Byte = 0
private const val COMPRESSION_GZIP: Byte = 1
private const val COMPRESSION_DEFLATE: Byte = 2
private const val COMPRESSION_SNAPPY: Byte = 3
private const val COMPRESSION_LZ4: Byte = 4

private const val PARSER_TYPE: Byte = 0
private const val PARSER_ASK_REQUEST_ID: Byte = 1
private const val PARSER_ACTOR_ASK_REQUEST_ID: Byte = 2
private const val PARSER_ACTOR_NAME_SIZE: Byte = 3
private const val PARSER_ACTOR_NAME_SIZE_FINISH: Byte = 4
private const val PARSER_ACTOR_NAME: Byte = 5
private const val PARSER_ACTION_NAME_SIZE: Byte = 6
private const val PARSER_ACTION_NAME_SIZE_FINISH: Byte = 7
private const val PARSER_ACTION_NAME: Byte = 8
private const val PARSER_TIMEOUT: Byte = 9

class PartitionTable(val size: Int) {

}

class CoreDB(val eventLoop: MEventLoop) {
   var current: MoveDB = MoveDBEmpty

   inner class Replica(val remoteCore: MClusterNode.Core) {
   }
}

object MoveDBEmpty : MoveDB {

}

interface MoveDB {

}

class CoreMDB(val maxSize: Long, val path: String) : MoveDB {
   val env: Env<ByteBuffer>

   val actorDb: Dbi<ByteBuffer>
   val actionDb: Dbi<ByteBuffer>

   val keyBuffer: ByteBuffer
   val valueBuffer: ByteBuffer

   var readTxn: Txn<ByteBuffer>
   var writeTxn: Txn<ByteBuffer>

   init {
      env = Env.create()
         .setMapSize(maxSize)
         .open(File(path))

      actorDb = env.openDbi(ACTOR_DB_NAME, DbiFlags.MDB_CREATE)
      actionDb = env.openDbi(ACTION_DB_NAME, DbiFlags.MDB_CREATE)

      keyBuffer = ByteBuffer.allocateDirect(env.maxKeySize)
      valueBuffer = ByteBuffer.allocateDirect(VALUE_BUFFER_SIZE)

      readTxn = env.txnRead()
      writeTxn = env.txnWrite()
   }

   fun lookupActor(actorID: ActorID) {

   }

   companion object {
      val ACTOR_DB_NAME = "a"
      val ACTION_DB_NAME = "i"
      val OUT_DB_NAME = "o"

      val VALUE_BUFFER_SIZE = 1024 * 1024 * 10
   }

   class Replica {
   }
}

const val VOLATILE: Byte = 0
const val NON_VOLATILE: Byte = 1

// 'Fast Action' are not replicated or persisted.
// If the system is too busy, then it returns an
// error indicating so.
const val DB_MODEL_READ: Byte = 0
// 'Actors' are usually somewhat long-living, but
// may also be very short lived like 'Presence'.
// 'Volatile' and 'Non-Volatile'
const val DB_MODEL_ACTOR: Byte = 1
// 'Delayed Worker' can be delayed some number of seconds.
// Or execution must be guaranteed in the future.
// Saved locally. Replication Required.
// Inherits Producer responsibility.
const val DB_MODEL_DELAY_WORKER: Byte = 2
// 'Worker' executes immediately pending available resources
// Caller will retry if failed.
// Saved locally. Replication not needed.
// Retry responsibility pushed back to the producer.
const val DB_MODEL_WORKER: Byte = 3

// Semi-Sync Replication is used to a quorum (2+ Replicas).
// Batching is used.
// Replication happens on the MEventLoop including LMDB database
// calls.
class ReplicationWorker {
   // Replicate Active Actors and Channel Backlog
   val actorIndex = 0L
   // Replicate Future Jobs - Delay
   val futureJobIndex = 0L
   // Replicate Running Jobs -
   val runningJobsIndex = 0L
}

interface ActorModelVisitor {
   fun visitID(buffer: ByteBuffer, position: Int, length: Int) {

   }

   fun visitID(id: ActorID)

   fun visitState(state: Byte)
}

const val ACTOR_STATE_STARTING: Byte = 1
const val ACTOR_STATE_RUNNING: Byte = 2
const val ACTOR_STATE_STOPPING: Byte = 3
const val ACTOR_STATE_STOPPED: Byte = 4
const val ACTOR_STATE_CRASHED: Byte = 5