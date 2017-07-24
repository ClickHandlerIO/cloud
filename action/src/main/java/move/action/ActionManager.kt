package move.action

import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import com.google.common.util.concurrent.AbstractIdleService
import io.vertx.rxjava.core.Vertx
import javaslang.control.Try
import move.cluster.HazelcastProvider
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central repository of all actions registered from ActionProviders.

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
      workerService.startAsync().awaitRunning()
      scheduledActionManager.startAsync().awaitRunning()
   }

   @Throws(Exception::class)
   override fun shutDown() {
      Try.run { scheduledActionManager.stopAsync().awaitTerminated() }
         .onFailure { e -> LOG.error("Failed to stop ScheduledActionManager", e) }
      Try.run { workerService.stopAsync().awaitTerminated() }
         .onFailure { e ->
            LOG.error(
               "Failed to stop WorkerService[" + workerService.javaClass.canonicalName + "]",
               e
            )
         }
   }

   companion object {
      private val LOG = LoggerFactory.getLogger(ActionManager::class.java)
      val threadPoolConfigs: Map<String, ThreadPoolConfig> = HashMap()
      val actionProviderMap = HashMap<Any, ActionProvider<Action<Any, Any>, Any, Any>>()
      val remoteActionMap = HashMap<Any, RemoteActionProvider<Action<Any, Any>, Any, Any>>()
      val internalActionMap = HashMap<Any, InternalActionProvider<Action<Any, Any>, Any, Any>>()
      val workerActionMap = HashMap<Any, WorkerActionProvider<Action<Any, Boolean>, Any>>()
      val workerActionQueueGroupMap: HashMap<String, List<WorkerActionProvider<Action<Any, Boolean>, Any>>> =
         LinkedHashMap<String, List<WorkerActionProvider<Action<Any, Boolean>, Any>>>()
      val scheduledActionMap = HashMap<Any, ScheduledActionProvider<Action<Unit, Unit>>>()
      var isWorker = true

      fun bindProducer(provider: WorkerActionProvider<*, *>, producer: WorkerProducer) {
         provider.producer = producer
      }

      fun setExecutionTimeoutEnabled(enabled: Boolean) {
         actionProviderMap.forEach { k, v -> v.isExecutionTimeoutEnabled = enabled }
      }

      fun getThreadPoolConfig(groupKey: String): ThreadPoolConfig? {
         return threadPoolConfigs[groupKey]
      }

      @Synchronized internal fun register(map: Map<Any, ActionProvider<Action<Any, Any>, Any, Any>>?) {
         if (map == null || map.isEmpty()) {
            return
         }

         actionProviderMap.putAll(map)

         map.forEach { key, value ->
            if (value.javaClass.isAssignableFrom(RemoteActionProvider::class.java)) {
               if (key is String) {
                  if (remoteActionMap.containsKey(key)) {
                     val actionProvider = remoteActionMap[key]
                     throw RuntimeException("Duplicate RemoteAction Entry for key [" + key + "]. " +
                        value.actionClass.canonicalName + " and " +
                        actionProvider!!.actionClass.canonicalName)
                  }
               }
               remoteActionMap.put(key, value as RemoteActionProvider<Action<Any, Any>, Any, Any>)
            } else if (value.javaClass.isAssignableFrom(InternalActionProvider::class.java)) {
               internalActionMap.put(key, value as InternalActionProvider<Action<Any, Any>, Any, Any>)
            } else if (value.javaClass.isAssignableFrom(WorkerActionProvider::class.java) || value.javaClass.isAssignableFrom(FifoWorkerActionProvider::class.java)) {
               workerActionMap.put(key, value as WorkerActionProvider<Action<Any, Boolean>, Any>)
               workerActionMap.put(value.actionClass.canonicalName, value)
               var list: List<WorkerActionProvider<Action<Any, Boolean>, Any>>? = workerActionQueueGroupMap.get(value.queueName)
               if (list == null) {
                  list = listOf()
                  workerActionQueueGroupMap.put(value.queueName, list)
               }
               list += value
            } else if (value.javaClass.isAssignableFrom(ScheduledActionProvider::class.java)) {
               scheduledActionMap.put(key, value as ScheduledActionProvider<Action<Unit, Unit>>)
            }
         }
      }
   }
}
