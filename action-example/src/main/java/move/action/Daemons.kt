package move.action

import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth
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
   lateinit var server: RemotingServerImpl

   suspend override fun startUp() {
      server = RemotingServerImpl()

      // Start and wait.
      server.start()
   }

   suspend override fun shutdown() {
      server.stop()
   }

   inner class RemotingServerImpl
      : RemotingServer(auth = JWTAuth.create(VERTX.delegate, JsonObject())) {

   }
}