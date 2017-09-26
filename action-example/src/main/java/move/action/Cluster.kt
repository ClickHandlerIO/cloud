package move.action

import com.hazelcast.config.*
import com.hazelcast.core.Hazelcast
import io.atomix.AtomixReplica
import io.atomix.catalyst.transport.Address
import io.atomix.catalyst.transport.local.LocalServerRegistry
import io.atomix.catalyst.transport.local.LocalTransport
import io.atomix.catalyst.transport.netty.NettyOptions
import io.atomix.catalyst.transport.netty.NettyTransport
import io.atomix.copycat.client.CopycatClient
import io.atomix.copycat.client.ServerSelectionStrategies
import io.atomix.copycat.server.storage.Storage
import io.atomix.copycat.server.storage.StorageLevel
import io.atomix.group.DistributedGroup
import io.atomix.group.LocalMember
import io.atomix.group.messaging.MessageProducer
import move.NUID
import net.jpountz.xxhash.XXHashFactory
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.CountDownLatch

object LZ4Helper {
   val xx = XXHashFactory.fastestInstance()

   @JvmStatic
   fun main(args: Array<String>) {
      val buf = ByteBuffer.allocateDirect(16)
      buf.putLong(1000L)
      buf.putLong(13345545435L)


//      xx.newStreamingHash32(0).update()
   }
}

/**
 *
 */
object Cluster {
   val LOG = LoggerFactory.getLogger(javaClass)

   val TOPIC = "topic"

   val PORT_1 = 8700
   val PORT_2 = 8701
   val PORT_3 = 8702

   lateinit var group: DistributedGroup
   lateinit var member: LocalMember
   lateinit var atomix: AtomixReplica

   val registry = LocalServerRegistry()
   val transport = LocalTransport(registry)


   @JvmStatic
   fun main(args: Array<String>) {
      System.setProperty("storage.compaction.threads", "1")
      first()
   }

   fun client() {
      val client = CopycatClient.builder()
         .withTransport(transport)
         .withServerSelectionStrategy(ServerSelectionStrategies.LEADER)
         .build()


   }

   fun first() {
      val props = Properties()
      props.setProperty(NettyOptions.THREADS, "1")


      atomix = AtomixReplica.builder(Address("localhost", PORT_1))
//         .withTransport(NettyTransport(NettyOptions(props)))
         .withTransport(transport)
         .withServerTransport(transport)
         .withClientTransport(transport)
         .withStorage(Storage(StorageLevel.MEMORY))
         .build()

      LOG.info("Build Atomix")
      atomix.bootstrap().get()

      LOG.info("Joining Group")
      group = atomix.getGroup("global").get()
      member = group.join().join()

      LOG.info("Getting Actor map")
      val map = atomix.getMap<String, String>("actor").join()

      LOG.info("Writing to Actor map")
      for (i in 0..10) {
         LOG.info(map.putIfAbsent(i.toString(), NUID.nextGlobal()).join())
      }

      val producer = member.messaging().producer<String>(
         TOPIC,
         MessageProducer.Options()
            .withDelivery(MessageProducer.Delivery.RANDOM)
            .withExecution(MessageProducer.Execution.ASYNC)
      )

//      LOG.info("Going to join second now")
//
//      second()

      Thread.sleep(10000000)
   }

   fun second(): AtomixReplica {
      val replica = AtomixReplica.builder(Address("localhost", PORT_2))
         .withType(AtomixReplica.Type.ACTIVE)
         .withTransport(NettyTransport())
         .withStorage(Storage(StorageLevel.MEMORY))
         .build()

      val joined = replica.join(Address("localhost", PORT_1)).get()

      execute(replica)

      return replica
   }

   fun execute(atomix: AtomixReplica) {
      val group = atomix.getGroup("global").join()
      val member = group.join().join()

      LOG.info("Members in Group: ${group.members().size}")

      member.messaging().consumer<String>(TOPIC).onMessage {

         //         it.ack()
//         it.ack()
//         it.reply("Back")
      }

//      LOG.info("Getting producer")
//      val producer = group.messaging().producer<String>("producer")

      LOG.info("Getting Actor map")
      val map = atomix.getMap<String, String>("actor").join()

      LOG.info("Writing to Actor map")
      for (i in 0..10) {
         LOG.info(map.get(i.toString()).join())
//         LOG.info(map.putIfAbsent(i.toString(), i.toString()).get())
      }

      for (p in 1..1000) {
         LOG.info("Reading from Map")
         val latch = CountDownLatch(100000)
         for (i in 1..100000) {
            map.get(2).thenAccept { latch.countDown() }
         }
         latch.await()
         LOG.info("Finished Reading from map")
      }

      LOG.info("Finished")
   }
}

object HZCluster {
   @JvmStatic
   fun main(args: Array<String>) {
      val config = Config("move")

      val props = Properties()
      props.setProperty("hazelcast.operation.thread.count", "1")
      props.setProperty("hazelcast.operation.generic.thread.count", "1")
      props.setProperty("hazelcast.event.thread.count", "1")
      props.setProperty("hazelcast.io.thread.count", "1")
      props.setProperty("hazelcast.io.balancer.interval.seconds", "-1")
      props.setProperty("hazelcast.logging.type", "slf4j")
      props.setProperty("com.hazelcast.level", "ERROR")
      props.setProperty("hazelcast.phone.home.enabled", "false")
      props.setProperty("hazelcast.slow.operation.detector.enabled", "false")
      props.setProperty("hazelcast.diagnostics.enabled", "false")
      props.setProperty("hazelcast.partition.migration.fragments.enabled", "false")
      props.setProperty("hazelcast.partition.max.parallel.replications", "0")
      props.setProperty("hazelcast.partition.count", "1")
      props.setProperty("hazelcast.map.invalidation.batch.enabled", "false")
      props.setProperty("hazelcast.io.input.thread.count", "1")
      props.setProperty("hazelcast.io.output.thread.count", "1")
      props.setProperty("hazelcast.cache.invalidation.batch.enabled", "false")
      props.setProperty("hazelcast.shutdownhook.enabled", "false")

      config.addExecutorConfig(ExecutorConfig("hz:system", 1))
      config.addExecutorConfig(ExecutorConfig("hz:scheduled", 1))
      config.addExecutorConfig(ExecutorConfig("hz:offloadable", 1))

      config.setProperties(props)
      config.networkConfig = NetworkConfig()
         .setJoin(JoinConfig()
            .setMulticastConfig(MulticastConfig()
               .setEnabled(false)
               .setLoopbackModeEnabled(false))
         )
      val hz = Hazelcast.getOrCreateHazelcastInstance(config)

      println("Local Address: ${hz.cluster.localMember.address}")
   }
}



























