package move.action

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
                     val actions: ActionStore) {

   init {
      // Setup JBoss logging provider.
      System.setProperty("org.jboss.logging.provider", "slf4j")
      // Setup IP stack. We want IPv4.
      System.setProperty("java.net.preferIPv6Addresses", "false")
      System.setProperty("java.net.preferIPv4Stack", "true")

      // Register actions from Dagger
      Companion.store = actions
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
