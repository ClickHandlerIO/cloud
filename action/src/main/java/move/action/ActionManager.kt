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
