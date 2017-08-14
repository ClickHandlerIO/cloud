package move.action

import com.google.common.util.concurrent.AbstractIdleService
import io.vertx.core.DeploymentOptions
import io.vertx.rxjava.core.AbstractVerticle
import io.vertx.rxjava.core.Vertx
import org.apache.ignite.IgniteCache
import org.apache.ignite.IgniteDataStreamer
import org.apache.ignite.Ignition
import org.apache.ignite.cache.CacheMode
import org.apache.ignite.cache.query.SqlFieldsQuery
import org.apache.ignite.cache.query.SqlQuery
import org.apache.ignite.cluster.ClusterNode
import org.apache.ignite.compute.ComputeJob
import org.apache.ignite.compute.ComputeJobResult
import org.apache.ignite.compute.ComputeJobResultPolicy
import org.apache.ignite.compute.ComputeTask
import org.apache.ignite.configuration.CacheConfiguration
import org.apache.ignite.configuration.IgniteConfiguration
import org.apache.ignite.configuration.PersistentStoreConfiguration
import org.apache.ignite.services.Service
import org.apache.ignite.services.ServiceConfiguration
import org.apache.ignite.services.ServiceContext
import java.util.*
import java.util.concurrent.TimeUnit


/**
 *
 */
class IgniteService : AbstractIdleService {
   constructor() : super()

   override fun startUp() {
      TODO("not implemented")
   }

   override fun shutDown() {
      TODO("not implemented")
   }

}


class UserService : Service {
   var running: Boolean = true

   override fun init(ctx: ServiceContext?) {
      Runner.ignite.message().localListen(Objects.toString(ctx?.affinityKey()), { e1: UUID?, e2: Any? ->
         println(e1?.toString())
         println(WorkerPacker.parse(e2 as ByteArray))
         running
      })
   }

   override fun cancel(ctx: ServiceContext?) {
      println("cancel")
      running = false
   }

   override fun execute(ctx: ServiceContext?) {
      println("execute")
   }
}

class WorkerTask : ComputeTask<Unit, Unit> {
   override fun reduce(results: MutableList<ComputeJobResult>?): Unit? {
      TODO("not implemented")
   }

   override fun result(res: ComputeJobResult?, rcvd: MutableList<ComputeJobResult>?): ComputeJobResultPolicy {
      TODO("not implemented")
   }

   override fun map(subgrid: MutableList<ClusterNode>?, arg: Unit?): MutableMap<out ComputeJob, ClusterNode> {
      TODO("not implemented")
   }
}

class MyVerticle : AbstractVerticle {
   constructor() : super()

   override fun start() {
      super.start()

      println("Starting MyVerticle")
   }

   override fun stop() {
      super.stop()

      println("Stopping MyVerticle")
   }
}

object Runner {
   val vertx = Vertx.vertx()
   val ignite = Ignition.start(buildConfig())

   @JvmStatic
   fun main(args: Array<String>) {
      ignite.active(true)

      val deploymentOptions = DeploymentOptions()
      deploymentOptions.instances = 1
      vertx.deployVerticle(MyVerticle::class.java.name)

      val userCache: IgniteCache<String, UserService> = ignite.getOrCreateCache("user")
      val srv = userCache.get("user1")

      val serviceConfig = ServiceConfiguration()
      serviceConfig.affinityKey = "user1"
      serviceConfig.cacheName = "user"
      serviceConfig.maxPerNodeCount = 1
      serviceConfig.service = UserService()
      serviceConfig.totalCount = 1
      serviceConfig.name = "user1"

      ignite.services().deployAsync(serviceConfig).listen {
         println(it)
      }

      val service = userCache.get("user1")

      val cache: IgniteCache<Long, UtConnection> = ignite.cache("connections")

      vertx.periodicStream(1000).handler {

         ignite.message().send("user1", WorkerPacker.byteify(WorkerEnvelope(0, "MyAction", 0, ByteArray(0))))
      }

      val streamer: IgniteDataStreamer<Long, UtConnection> = ignite.dataStreamer("connections")
      streamer.allowOverwrite(true)

//      for (i in 0..99999) {
//         val id = i.toString()
//         streamer.addData(i.toLong(), UtConnection(i.toLong()))
//
//         if (i > 0 && i % 10000 == 0)
//            println("Done: " + i)
//      }

//      val cluster = ignite.cluster()
//
//      // Get compute instance which will only execute
//      // over remote nodes, i.e. not this node.
      val compute = ignite.compute()



      vertx.periodicStream(5000).handler {
         for (i in 0..10000) {
            compute.affinityRunAsync("user", "user_1", {
               if (i > 0 && i % 10000 == 0)
                  println("Run Async Done: " + i)
            })
         }
      }

//
//      // Broadcast to all remote nodes and print the ID of the node
//      // on which this closure is executing.
//      compute.broadcast {
//         println("Hello Node: " + ignite.cluster().localNode().id())
//      }
   }

   fun buildConfig(): IgniteConfiguration {
      val config = IgniteConfiguration()

      val persistentStoreConfig = PersistentStoreConfiguration()
//      config.persistentStoreConfiguration = persistentStoreConfig

      val cacheConfig = CacheConfiguration<String, UtConnection>("connections")
      cacheConfig.setIndexedTypes(String.javaClass, UtConnection::class.java)
      cacheConfig.setCacheMode(CacheMode.PARTITIONED)

      config.setCacheConfiguration(cacheConfig)

      return config
   }
}

object RunClient {
   @JvmStatic
   fun main(args: Array<String>) {
      Ignition.setClientMode(true)

      val ignite = Ignition.start(Runner.buildConfig())

      val cache: IgniteCache<String, UtConnection> = ignite.cache("connections")

      var total = 0L

      val count = cache.query(SqlFieldsQuery("SELECT COUNT(*) FROM UTCONNECTION")).all

      for (i in 0..9999) {
         val start = System.nanoTime()
         val query = SqlQuery<UtConnection, String>(UtConnection::class.java, "SELECT * FROM UTCONNECTION LIMIT 10")
         query.setDistributedJoins(true)

         val cursor = cache.query(query)
         val all = cursor.all
         val end = System.nanoTime() - start

         total += end

         if (i > 0 && i % 100 == 0)
            println("Avg Query Time: " + (TimeUnit.NANOSECONDS.toMillis(total).toDouble() / i) + " millis")
      }

//      val streamer:IgniteDataStreamer<String, UtConnection> = ignite.dataStreamer("connections")
//      streamer.allowOverwrite(true)
//
//      for (i in 0..99999) {
//         val id = UID.next()
//         streamer.addData(id, UtConnection(id))
//
//         if (i > 0 && i % 10000 == 0)
//            println("Done: " + i)
//      }
   }
}

data class UtConnection(val id: Long, val created: Long = System.currentTimeMillis())
