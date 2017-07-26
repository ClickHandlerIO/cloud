package move.action

import com.google.common.util.concurrent.AbstractIdleService
import io.vertx.rxjava.core.Vertx
import javaslang.control.Try
import move.cluster.HazelcastProvider
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central repository of all actions.

 * @author Clay Molocznik
 */
@Singleton
class ActionManager @Inject
internal constructor(val vertx: Vertx,
                     val hazelcastProvider: HazelcastProvider,
                     val workerService: WorkerService,
                     val scheduledActionManager: ScheduledActionManager) : AbstractIdleService() {

   @Throws(Exception::class)
   override fun startUp() {
      // Startup worker service.
      workerService.startAsync().awaitRunning()
      // Startup scheduled actions.
      scheduledActionManager.startAsync().awaitRunning()
   }

   @Throws(Exception::class)
   override fun shutDown() {
      // Shutdown scheduled actions.
      Try.run { scheduledActionManager.stopAsync().awaitTerminated() }
         .onFailure { e -> LOG.error("Failed to stop ScheduledActionManager", e) }

      // Shutdown worker service.
      Try.run { workerService.stopAsync().awaitTerminated() }
         .onFailure { e ->
            LOG.error(
               "Failed to stop WorkerService[" + workerService.javaClass.canonicalName + "]",
               e
            )
         }
   }

   companion object : ActionLocator() {
      private val LOG = LoggerFactory.getLogger(ActionManager::class.java)
   }
}
