package move.action

import org.agrona.ExpandableDirectByteBuffer
import org.lmdbjava.*
import org.rocksdb.Options
import org.rocksdb.RocksDB
import org.rocksdb.RocksDBException
import java.io.File
import java.nio.ByteBuffer

/**
 *
 */
object Rocks {
   @JvmStatic
   fun main(args: Array<String>) {
      // a static method that loads the RocksDB C++ library.
      RocksDB.loadLibrary();

      // the Options class contains a set of configurable DB options
      // that determines the behaviour of the database.
      val options = Options().setCreateIfMissing(true)
      try {

         // a factory method that returns a RocksDB instance
         val db = RocksDB.open(options, System.getProperty("user.home") + File.separator + "playground" + File.separator + "rocks")

         val buf = ByteBuffer.allocate(8)
         val bufV = ByteBuffer.allocate(8)

         for (b in 1..10) {
            val start = System.currentTimeMillis();
            for (p in 1..1000000) {
               buf.putLong(p.toLong())
               buf.flip()

               bufV.putLong(p.toLong())
               bufV.flip()

               db.put(buf.array(), bufV.array())
            }
            println("${System.currentTimeMillis() - start}")
         }
      } catch (e: RocksDBException) {
         // do some error handling
         e.printStackTrace()
      }
   }
}

object MLMDB {
   val ACTOR_DB_NAME = "a"
   val IN_DB_NAME = "i"
   val OUT_DB_NAME = "o"

   @JvmStatic
   fun main(args: Array<String>) {
      println(Runtime.getRuntime().maxMemory())
      println(Runtime.getRuntime().freeMemory())

      val path = File(System.getProperty("user.home") + "/playground/lmdb")

      // We always need an Env. An Env owns a physical on-disk storage file. One
      // Env can store many different databases (ie sorted maps).
      val env = Env.create()
         // LMDB also needs to know how large our DB might be. Over-estimating is OK.
         .setMapSize(1024 * 1024 * 512)
         // LMDB also needs to know how many DBs (Dbi) we want to store in this Env.
         .setMaxDbs(1)
         // Now let's open the Env. The same path can be concurrently opened and
         // used in different processes, but do not open the same path twice in
         // the same process at the same time.
         .open(path)

      val keyBuffer = ExpandableDirectByteBuffer(env.maxKeySize)
      val valueBuffer = ExpandableDirectByteBuffer(1024 * 1024 * 10)

      val db = env.openDbi(IN_DB_NAME, DbiFlags.MDB_CREATE)

//      val buf = ExpandableDirectByteBuffer(8)
//      val bufV = ExpandableDirectByteBuffer(8)

      val buf = ByteBuffer.allocateDirect(8)
      val bufV = ByteBuffer.allocateDirect(8)

      buf.putLong(0, 1001)

      val value = env.txnRead().use { db.get(it, buf) }

//      println(value.getLong(0))

      for (b in 1..10) {
         env.txnWrite().use {
            val start = System.currentTimeMillis()
            for (p in 1..1000000) {
               buf.putLong(0, p.toLong())
               bufV.putLong(0, p.toLong() * 2)

               val saved = db.put(it, buf, bufV, PutFlags.MDB_APPEND, PutFlags.MDB_NOOVERWRITE)

//               println(saved)
            }
            it.commit()
            println("${System.currentTimeMillis() - start}")
         }
      }
   }
}



