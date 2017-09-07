package move.action

import com.google.common.util.concurrent.AbstractIdleService
import io.vertx.rxjava.core.Vertx
import move.NUID
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

val Actions
   get() = ActionManager.Companion.store

/**
 * Central repository of all actions.

 * @author Clay Molocznik
 */
@Singleton
class ActionManager @Inject
internal constructor(val vertx: Vertx,
                     val actions: ActionStore) : AbstractIdleService() {

   // Does this node accept HTTP and WebSocket requests?
   var isRemote = true
   // Does this node handle work?
   var isWorker = true

   var timerID: Long = -1L

   var remoteServer: RemoteServer? = null

   init {
      // Setup JBoss logging provider.
      System.setProperty("org.jboss.logging.provider", "slf4j")
      // Setup IP stack. We want IPv4.
      System.setProperty("java.net.preferIPv6Addresses", "false")
      System.setProperty("java.net.preferIPv4Stack", "true")

      // Register actions from Dagger
      Companion.store = actions

//      // Start stats timer
//      timerID = vertx.delegate.setPeriodic(1000L) { publishStats() }
//      (vertx.delegate as VertxInternal).addCloseHook {
//         vertx.delegate.cancelTimer(timerID)
//      }
   }

   override fun startUp() {
   }

   override fun shutDown() {
   }

   private fun publishStats() {
      vertx.rxExecuteBlocking<Unit> {

      }.subscribe()
   }

   companion object {
      internal lateinit var store: ActionStore
         get
         private set

      private val LOG = LoggerFactory.getLogger(ActionManager::class.java)
      private var brokers: Map<Class<*>, ActionBroker> = mapOf()

      private var nodeId: String = NUID.nextGlobal()

      val NODE_ID
         get() = nodeId
   }
}
