package move.action

import io.netty.buffer.ByteBuf
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.AsyncResult
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.http.ServerWebSocket
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicLong

/**
 *
 * @author Clay Molocznik
 */
open class RemoteServer(val port: Int = 15000, val host: String = "", var auth: JWTAuth) {
   var httpServer: HttpServer? = null
   val counter = AtomicLong(0L)

   // Create map with large initial capcity to ensure we don't have
   // long "re-hashing" delays until extreme load.
   // Supports 1.5 million.
   val webSocketMap: ConcurrentMap<Long, WS> = ConcurrentHashMap<Long, WS>(
      if (MOVE.mode == Mode.PROD) 2_000_000 else 10,
      0.75f,
      Runtime.getRuntime().availableProcessors() * 2
   )

   var maxWebSockets = MAX_WEBSOCKETS
   var maxInflightPerWebSocket = MAX_WEBSOCKET_INFLIGHT

   init {
   }

   suspend fun start() {
      httpServer = buildHttpServer()
      awaitEvent<AsyncResult<HttpServer>> { r -> httpServer?.listen { r.handle(it) } }
   }

   suspend fun stop() {
      awaitEvent<AsyncResult<Void>> { r -> httpServer?.close { r.handle(it) } }
   }

   fun buildHttpServer(): HttpServer {
      val router = Router.router(VERTX.delegate)

      Actions.http
         .filter { it.visibleTo(ActionVisibility.PUBLIC) }
         .forEach { producer ->
            val annotation = producer.provider.annotation ?:
               throw RuntimeException(
                  "HttpAction [${producer.provider.actionClass.canonicalName}] missing @Http"
               )

            var route = if (annotation.path.isBlank())
               router.route()
            else
               router.route(annotation.path)

            when (annotation.method) {
               Http.Method.CONNECT -> route.method(HttpMethod.CONNECT)
               Http.Method.DELETE -> route.method(HttpMethod.DELETE)
               Http.Method.GET -> route.method(HttpMethod.GET)
               Http.Method.HEAD -> route.method(HttpMethod.HEAD)
               Http.Method.OPTIONS -> route.method(HttpMethod.OPTIONS)
               Http.Method.POST -> route.method(HttpMethod.POST)
               Http.Method.PATCH -> route.method(HttpMethod.PATCH)
               Http.Method.PUT -> route.method(HttpMethod.PUT)
               Http.Method.TRACE -> route.method(HttpMethod.TRACE)
               Http.Method.ALL -> route
            }

            annotation.produces.forEach { route.produces(it) }
            annotation.consumes.forEach { route.consumes(it) }

            // Create handler.
            route.handler { routingContext ->
               // Ask
               producer.rxAsk(request = routingContext).asSingle().subscribe(
                  {
                     if (!routingContext.response().ended()) {
                        onIncompleteResponse(producer, routingContext)
                     }
                  },
                  { onException(producer, routingContext, it) }
               )
            }
         }

      router.route().handler(this::catchAll)

      return VERTX.delegate
         .createHttpServer(buildHttpOptions())
         .websocketHandler(this::websocket)
         .requestHandler(router::accept)
   }

   protected open fun onIncompleteResponse(producer: HttpActionProducer<*, *>,
                                           context: RoutingContext) {
      context.response().setStatusCode(500).end()
   }

   protected open fun onException(producer: HttpActionProducer<*, *>,
                                  context: RoutingContext,
                                  cause: Throwable) {
      context.response().setStatusCode(500).end(cause.localizedMessage)
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
      val presence: Map<String, Any> by lazy { HashMap<String, Any>() }
      val expires: Long = 0
      var paused = false

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
         webSocketMap.remove(id)
      }

      fun handleException(exception: Throwable) {
         webSocket.close()
      }

      fun handleText(message: String) {
         // Unpack message.
         incInFlight()
      }

      fun handleBinary(buffer: Buffer) {
         incInFlight()
      }

      private fun incInFlight() {
         inFlight++
         trafficControl()
      }

      private fun decInFlight() {
         inFlight--
         if (inFlight < 0)
            inFlight = 0

         trafficControl()
      }

      fun trafficControl() {
         if (inFlight >= maxInflightPerWebSocket) {
            paused = true
            webSocket.pause()
         } else if (paused) {
            paused = false
            webSocket.resume()
         }
      }
   }
}

data class VerifyResult(
   /**
    *
    */
   val tokenToPass: String?,
   /**
    * Epoch in millis when token expires.
    */
   val expires: Long,
   /**
    *
    */
   val maxInFlight: Int = 0,
   /**
    * The unique ID of user or system
    */
   val userId: String? = null,
   /**
    * Limit the number of connections the user can have.
    * This is across the entire network.
    */
   val maxConnections: Int = 0
)

interface RemoteVerifier {
   fun verify(token: String): VerifyResult
}

data class RemoteEnvelope(
   /**
    *
    */
   val direction: Int = 1,
   // This is the ID of the request.
   // Full asynchronous pipelining is used
   val id: Long,
   // This maps to a path or action name.
   val name: String,
   /**
    *
    */
   val timeout: Long,
   /**
    *
    */
   val payload: Buffer
)

/**
 * Wire format for remoting.
 */
object RemotePacker {

}

enum class RemoteMessageType {
   ASK,
   ASK_REPLY,
   PRESENCE_GET,
   PRESENCE_LEAVE,
   PRESENCE_STATE,
   PRESENCE_CHANGE,
   STREAM,
}


data class AskMsg(
   val id: Long,
   val payload: ByteArray
)

data class AskReplyMsg(
   val id: Long,
   val code: Int,
   val payload: ByteArray
)

data class StreamMsg(
   val id: Long,
   val seq: Long,
   val payload: ByteBuf
)