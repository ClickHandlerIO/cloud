package move.action

import org.slf4j.LoggerFactory
import javax.inject.Singleton

/**
 *
 */
@Daemon(
   // Only start for REMOTE role.
   role = NodeRole.REMOTE,
   // Ensure it's at the end.
   order = Int.MAX_VALUE
)
@Singleton
class WebServerDaemon : AbstractDaemon() {
   val log = LoggerFactory.getLogger(javaClass)

   lateinit var server: RemoteServerImpl

   suspend override fun startUp() {
      log.info("starting")
      server = RemoteServerImpl()

      // Start and wait.
      server.start()
   }

   suspend override fun shutdown() {
      log.info("stopping")
      server.stop()
   }

   inner class RemoteServerImpl
      : RemoteServer() {

   }
}


/**
 *
 */
@Daemon(
   // Only start for REMOTE role.
   role = NodeRole.ALL,
   // Ensure it's at the end.
   order = 0
)
@Singleton
class UserStore : AbstractDaemon() {
   val log = LoggerFactory.getLogger(javaClass)

   suspend override fun startUp() {
      log.info("starting")
   }

   suspend override fun shutdown() {
      log.info("stopping")
   }
}
