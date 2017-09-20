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
   lateinit var server: RemoteServerImpl

   suspend override fun startUp() {
      server = RemoteServerImpl()

      // Start and wait.
      server.start()
   }

   suspend override fun shutdown() {
      server.stop()
   }

   suspend override fun onTimer(msg: TimerMessage) {
      // Clean WebSockets.
   }

   inner class RemoteServerImpl
      : RemoteServer(auth = JWTAuth.create(VERTX.delegate, JsonObject())) {

   }
}