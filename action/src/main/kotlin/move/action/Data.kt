package move.action

import io.netty.buffer.ByteBuf
import net.jpountz.lz4.LZ4Compressor
import net.jpountz.lz4.LZ4Factory
import net.jpountz.lz4.LZ4FastDecompressor
import org.lmdbjava.Dbi
import org.lmdbjava.DbiFlags
import org.lmdbjava.Env
import org.lmdbjava.Txn
import java.io.File
import java.nio.ByteBuffer

// ReplicaCore
//


/**
 * Actor goes through multiple stages
 *
 * ACK -> Replicated with Core
 * Starting
 * Running
 * Stopped
 *
 * Local NODE keeps track of Running Actors
 */
class ActorLog {
   class Started {

   }

   class Stopped {

   }
}

/**
 * Global Actor Pointer.
 */
interface ActorRegistry {
//   fun locate(id: ActionName): MClusterNodeCore
}

/**
 * Local ActorDB
 * Replicated with Core Replicas
 */
object ActorDB {
   val env = MDBEnv(Env
      .create()
      .setMapSize(10_000_000)
      .setMaxDbs(Runtime.getRuntime().availableProcessors() * 50)
      .open(File.createTempFile("move", "db"))
   )

   val db = env.db("a")
}

/**
 *
 * Replicated with Core Replicas
 */
object ActionDB {

}

/**
 * Processes Actions for a core in order.
 * Saved and replicated.
 */
class CoreProcessor {

}

interface DirectMemoryBacked {

}

class DirectParticipant(val buf: ByteBuf) {
   companion object {
      val USER_ID_INDEX = 0
      val CREATED_INDEX = 1
   }
}

class MDBEnv(val env: Env<ByteBuffer>) {
   private val databases = mutableMapOf<String, DB>()

   val write get() = env.txnWrite()
   val read get() = env.txnRead()

   @Synchronized
   fun db(name: String): DB {
      return databases.computeIfAbsent(name, { DB(env.openDbi(name, DbiFlags.MDB_CREATE)) })
   }

   @Synchronized
   fun close() {
      databases.values.forEach { it.db.close() }
      env.close()
   }

   inner class DB(val db: Dbi<ByteBuffer>) {
      fun get(txn: Txn<ByteBuffer>, key: ByteBuffer): ByteBuffer {
         return db.get(txn, key)
      }

      fun put(txn: Txn<ByteBuffer>, key: ByteBuffer, value: ByteBuffer): Boolean {
         return db.put(txn, key, value)
      }
   }
}

/**
 *
 */
object Compression {
   val lZ4Factory = LZ4Factory.fastestInstance()

   fun compressor() = LZ4X(lZ4Factory.fastCompressor(), lZ4Factory.fastDecompressor())
}

interface Compressor {
   fun compress(src: ByteBuf, dst: ByteBuf)

   fun compress(src: ByteArray): ByteArray

   fun decompress(src: ByteBuf, dst: ByteBuf)
}

class LZ4X(val compressor: LZ4Compressor,
           val decompressor: LZ4FastDecompressor) : Compressor {
   override fun compress(src: ByteBuf, dst: ByteBuf) {
      compressor.compress(src.nioBuffer(), dst.nioBuffer())
   }

   override fun compress(src: ByteArray): ByteArray {
      return compressor.compress(src)
   }

   override fun decompress(src: ByteBuf, dst: ByteBuf) {
      decompressor.decompress(src.nioBuffer(), dst.nioBuffer())
   }
}