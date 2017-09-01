package move.action

import com.google.common.util.concurrent.AbstractIdleService
import io.vertx.rxjava.core.Vertx
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
                     val actions: ActionMap) : AbstractIdleService() {

   init {
      // Setup JBoss logging provider for Undertow.
      System.setProperty("org.jboss.logging.provider", "slf4j")
      // Setup IP stack. We want IPv4.
      System.setProperty("java.net.preferIPv6Addresses", "false")
      System.setProperty("java.net.preferIPv4Stack", "true")

      put(actions.map)
      ensureActionMap()
   }

   override fun startUp() {
   }

   override fun shutDown() {
   }

   companion object : ActionLocator() {
      private val LOG = LoggerFactory.getLogger(ActionManager::class.java)
   }
}
