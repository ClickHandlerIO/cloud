package move.action

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.http.ServerWebSocket
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import net.openhft.chronicle.queue.ChronicleQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicLong

/**
 *
 * @author Clay Molocznik
 */
open class RemoteServer(val port: Int = 15000, val host: String = "", var auth: JWTAuth) : AbstractVerticle() {
   var httpServer: HttpServer? = null
   val counter = AtomicLong(0L)

   // Create map with large initial capcity to ensure we don't have
   // long "re-hashing" delays until extreme load.
   val webSocketMap: ConcurrentMap<Long, WS> = ConcurrentHashMap<Long, WS>(
      1_000_000,
      0.75f,
      Runtime.getRuntime().availableProcessors() * 2
   )

   var maxWebSockets = MAX_WEBSOCKETS
   var maxInflightPerWebSocket = MAX_WEBSOCKET_INFLIGHT

   // Every request is logged.
   // Connection request
   // Connection response
   // Action request
   // Action response
   // Streams may optionally be logged
   var requestLog: ChronicleQueue? = null

   init {

   }

   override fun start(startFuture: Future<Void>?) {
      val actions = ActionManager.actionMap.filter {
         it.value.visibility == Remote.Visibility.PUBLIC
      }.map { it.value }.toList()

      httpServer = buildHttpServer()
      httpServer?.listen {
         if (it.failed())
            startFuture?.fail(it.cause())
         else
            startFuture?.complete()
      } ?: startFuture?.fail("HttpServer was not returned from buildHttpServer")
   }

   override fun stop(stopFuture: Future<Void>?) {
      httpServer?.close {
         if (it.failed())
            stopFuture?.fail(it.cause())
         else
            stopFuture?.complete()
      } ?: stopFuture?.complete()
   }

   fun buildHttpServer(): HttpServer {
      val router = Router.router(vertx)
      router.route().handler(this::catchAll)

      return vertx
         .createHttpServer(buildHttpOptions())
         .websocketHandler(this::websocket)
         .requestHandler(router::accept)
   }

   protected open fun buildHttpOptions(): HttpServerOptions {
      val options = HttpServerOptions()
         .setPort(port)
         .setUsePooledBuffers(true)
         .setMaxChunkSize(MAX_CHUNK)
         .setCompressionSupported(true)
         .setDecompressionSupported(true)
         .setMaxWebsocketFrameSize(MAX_FRAME)
         .setMaxWebsocketMessageSize(MAX_MESSAGE)

      if (!host.isBlank()) {
         options.setHost(host)
      }

      return options
   }

   protected open fun websocket(webSocket: ServerWebSocket) {
      // Have we hit our websocket limit?
      if (webSocketMap.size >= maxWebSockets) {
         webSocket.reject()
         return
      }

      val id = counter.incrementAndGet()
      val conn = WS(id = id, webSocket = webSocket)

      webSocketMap.put(id, conn)
   }

   protected open fun catchAll(routingContext: RoutingContext) {
      routingContext
         .response()
         .setStatusCode(HttpResponseStatus.NOT_FOUND.code())
         .end("???")
   }

   companion object {
      val MAX_CHUNK = Character.MAX_VALUE.toInt()
      val MAX_FRAME = MAX_CHUNK
      val MAX_MESSAGE = MAX_FRAME * 64
      val X_TOKEN = "X-Token"
      val MAX_WEBSOCKET_INFLIGHT = 100
      val MAX_WEBSOCKETS = 1_000_000
   }

   inner class WS(val id: Long, val webSocket: ServerWebSocket) {
      var inFlight = 0
      var counter = 0
      // Lazily initialize streams map.
      val streams: Map<Long, Any> by lazy { HashMap<Long, Any>() }
      val expires: Long = 0

      init {
         webSocket.textMessageHandler(this::handleText)
         webSocket.binaryMessageHandler(this::handleBinary)
         webSocket.exceptionHandler(this::handleException)
         webSocket.closeHandler(this::handleClose)
      }

      fun validate() {
         // Find JWT token
         val query = webSocket.query().orEmpty()

         if (query.isNotEmpty()) {

         }
         webSocket.headers()[X_TOKEN]
      }

      fun handleClose(event: Void) {

      }

      fun handleException(exception: Throwable) {
         webSocket.close()
      }

      fun handleText(message: String) {
         // Unpack message.
         inFlight++

         if (inFlight >= maxInflightPerWebSocket) {
            webSocket.pause()
         }
      }

      fun handleBinary(buffer: Buffer) {
         inFlight++
      }

      fun trafficControl() {
         if (inFlight >= maxInflightPerWebSocket) {
            webSocket.pause()
         }
      }
   }
}

interface RemoteTokenVerifier {
   fun verify(token: String)
}

data class RemoteEnvelope(
   val direction: Int = 1,
   // This is the ID of the request.
   // Full asynchronous pipelining is used
   val id: Long,
   // This maps to a path or action name.
   val name: String,
   val timeout: Long,
   val payload: Buffer
)

object RemotePacker {

}