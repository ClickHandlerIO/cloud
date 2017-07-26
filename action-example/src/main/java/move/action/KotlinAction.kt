package move.action

import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.async


/**
 * @author Clay Molocznik
 */
object KotlinAction {
   @JvmStatic
   fun main(args: Array<String>) {
      // Setup JBoss logging provider for Undertow.
      System.setProperty("org.jboss.logging.provider", "slf4j")
      // Setup IP stack. We want IPv4.
      System.setProperty("java.net.preferIPv6Addresses", "false")
      System.setProperty("java.net.preferIPv4Stack", "true")

//        AppComponent.instance.db().startAsync().awaitRunning()

      val Actions: Action_Locator = AppComponent.instance.actions().move.action
      Actions.ensureActionMap()
      AppComponent.instance.actionManager().startAsync().awaitRunning()

      val executor = AppComponent.instance.vertx().createSharedWorkerExecutor("db", 10, Integer.MAX_VALUE.toLong());

//        actions().register()
      async(Unconfined) {
         try {
            val result = Actions.allocateInventory { id = "" }

            println(result.code)
         } catch (e: Throwable) {
            e.printStackTrace()
         }
      }
   }
}


object Another {
   @JvmStatic
   fun main(args: Array<String>) {
      println("")
   }
}
