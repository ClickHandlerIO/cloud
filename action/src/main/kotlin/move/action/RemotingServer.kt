package move.action

import com.google.common.base.Throwables
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
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

const val TIMER_TYPE_EVICTION = 1
const val TIMER_TYPE_PING = 2
const val TIMER_TYPE_PONG = 3

/**
 *
 * @author Clay Molocznik
 */
open class RemotingServer(val port: Int = 15000, val host: String = "", var auth: JWTAuth) {
   var httpServer: HttpServer? = null
   val idCounter = AtomicLong(0L)
   val webSocketCounter = AtomicLong(0L)
   var maxWebSockets = MAX_WEBSOCKETS
   var maxInflightPerWebSocket = MAX_WEBSOCKET_INFLIGHT

   suspend fun start() {
      // Create a WebSocket map for each EventLoop.
      MoveKernel.initEventLoops { MAP.set(LongSkipListMap()) }

      httpServer = buildHttpServer()
      awaitEvent<AsyncResult<HttpServer>> { r -> httpServer?.listen { r.handle(it) } }
   }

   suspend fun stop() {
      awaitEvent<AsyncResult<Void>> { r -> httpServer?.close { r.handle(it) } }
   }

   suspend fun buildHttpServer(): HttpServer {
      val router = Router.router(VERTX.delegate)

      Actions.http
         .filter { include(it) }
         .forEach { producer ->
            val annotation = producer.provider.annotation

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
            route.handler { request ->
               // Ask
               producer.rxAsk(request = request).asSingle().subscribe(
                  {
                     if (!request.response().ended()) {
                        onIncompleteResponse(producer, request)
                     }
                  },
                  { onUncaughtException(producer, request, it) }
               )
            }
         }

      // Add HTTP handlers for all public workers.
      Actions.worker
         .filter { include(it) }
         .forEach {

         }

      // Add any other handlers.
      addHandlers(router)

      // Add "Catch-All" handler last.
      router.route().handler(this::catchAll)

      return VERTX.delegate
         .createHttpServer(buildHttpOptions())
         .websocketHandler(this::onWebSocket)
         .requestHandler(router::accept)
   }

   protected open fun include(producer: HttpActionProducer<*, *>) =
      producer.visibleTo(ActionVisibility.PUBLIC)

   protected open fun include(producer: WorkerActionProducer<*, *, *, *>) =
      producer.provider.visibility == ActionVisibility.PUBLIC

   suspend protected open fun addHandlers(router: Router) {
   }

   protected open fun onIncompleteResponse(producer: HttpActionProducer<*, *>,
                                           context: RoutingContext) {

      LOG.error(
         """
         onIncompleteResponse() for [${producer.provider.actionClass.canonicalName}]
         Returned from HttpAction before calling 'resp.end()'
         "Responding with '500 - Internal Server Error'
         """.trimIndent()
      )
      context.response().setStatusCode(500).end()
   }

   protected open fun onUncaughtException(producer: HttpActionProducer<*, *>,
                                          context: RoutingContext,
                                          cause: Throwable) {
      LOG.error(
         """
         onUncaughtException() for [${producer.provider.actionClass.canonicalName}]
         Exception was not handled within the action.
         """.trimIndent(),
         Throwables.getRootCause(cause)
      )
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

   protected open fun catchAll(routingContext: RoutingContext) {
      routingContext
         .response()
         .setStatusCode(HttpResponseStatus.NOT_FOUND.code())
         .end("???")
   }

   protected open fun onWebSocket(webSocket: ServerWebSocket) {
      var eventLoop = MoveKernel.currentEventLoop

      // Have we hit our websocket limit?
      if (webSocketCounter.get() >= maxWebSockets) {
         webSocket.reject()
         return
      }

      val id = idCounter.incrementAndGet()
      val conn = WS(id = id, webSocket = webSocket, eventLoop = eventLoop)



      webSocketCounter.incrementAndGet()
      MAP.get().put(id, conn)
   }

   fun evict() {
      MoveKernel.forEachExecute { evict(it) }
   }

   private fun evict(loop: MoveEventLoop) {

   }

   companion object {
      val LOG = LoggerFactory.getLogger(RemotingServer::class.java)
      val MAX_CHUNK = Character.MAX_VALUE.toInt()
      val MAX_FRAME = MAX_CHUNK
      val MAX_MESSAGE = MAX_FRAME * 64
      val X_TOKEN = "X-Token"
      val MAX_WEBSOCKET_INFLIGHT = 256
      val MAX_WEBSOCKETS = 1_000_000
      val PING_INTERVAL_SECONDS = 30L
      val PONG_TIMEOUT_SECONDS = 5L

      val MAP = ThreadLocal<LongSkipListMap<WS>>()

   }


   inner class WS(val id: Long,
                  val webSocket: ServerWebSocket,
                  val eventLoop: MoveEventLoop) : HasTimers {

      var inFlight = 0
      var counter = 0
      val expires: Long = 0
      var lastActivity = eventLoop.epoch

      // Streams map
      var streams: Map<Long, Any> = emptyMap()
      // Presence map
      var presence: Map<String, Any> = emptyMap()

      var paused = false

      var expirationTimer: TimerEventHandle? = null
      var pingTimer: TimerEventHandle? = null
      var pongTimer: TimerEventHandle? = null

      var pingEpoch = -1L

      var closed = false

      // Round-trip latency measured by PING-PONG keep-alive.
      var latency = -1L
      var minLatency = -1L
      var maxLatency = -1L

      override fun addTimer(handle: TimerHandle) {
         if (handle is TimerEventHandle) {
            when (handle.type) {
               TIMER_TYPE_EVICTION -> expirationTimer = handle
               TIMER_TYPE_PING -> pingTimer = handle
               TIMER_TYPE_PONG -> pongTimer = handle
            }
         }
      }

      override fun removeTimer(handle: TimerHandle) {
         if (handle is TimerEventHandle) {
            when (handle.type) {
               TIMER_TYPE_EVICTION -> expirationTimer = null
               TIMER_TYPE_PING -> pingTimer = null
               TIMER_TYPE_PONG -> pongTimer = null
            }
         }
      }

      override fun onTimer(handle: TimerEventHandle) {
         when (handle.type) {
            TIMER_TYPE_EVICTION -> evict()
            TIMER_TYPE_PING -> sendPing()
            TIMER_TYPE_PONG -> onPongTimeout()
         }
      }

      init {
         webSocket.textMessageHandler(this::handleText)
         webSocket.binaryMessageHandler(this::handleBinary)
         webSocket.exceptionHandler(this::handleException)
         webSocket.closeHandler(this::handleClose)

         // Schedule eviction.
         scheduleEviction()

         // Schedule first ping.
         schedulePing()
      }

      private fun scheduleEviction() {
         // Evict in 1 day.
         eventLoop.scheduleTimer(
            TIMER_TYPE_EVICTION,
            this,
            TimeUnit.HOURS.toMillis(24)
         )
      }

      private fun schedulePing(waitSeconds: Long = PING_INTERVAL_SECONDS) {
         // Schedule the next PING in 30 seconds.
         eventLoop.scheduleTimer(
            TIMER_TYPE_PING,
            this,
            TimeUnit.SECONDS.toMillis(waitSeconds)
         )
      }

      private fun schedulePong() {
         // Schedule the PONG timeout.
         eventLoop.scheduleTimer(
            TIMER_TYPE_PING,
            this,
            TimeUnit.SECONDS.toMillis(PONG_TIMEOUT_SECONDS)
         )
      }

      private fun evict() {
         // Ensure WebSocket is closed.
         webSocket.close()
      }

      private fun sendPing() {
         try {
            pingEpoch = System.currentTimeMillis()
            // TODO: Send message

            schedulePong()
         } catch (e: Throwable) {
            if (!closed) {
               schedulePing(1)
            }
         }
      }

      private fun receivePong() {
         try {
            latency = System.currentTimeMillis() - pingEpoch
            minLatency = if (minLatency == -1L) {
               latency
            } else {
               Math.min(minLatency, latency)
            }
            maxLatency = if (maxLatency == -1L) {
               latency
            } else {
               Math.max(maxLatency, latency)
            }
         } finally {
            schedulePing()
         }
      }

      private fun onPongTimeout() {
         if (LOG.isDebugEnabled)
            LOG.debug("Timed-out waiting for PONG. Closing WebSocket...")

         webSocket.close()
      }

      fun validate() {
         // Find JWT token
         val query = webSocket.query().orEmpty()

         if (query.isNotEmpty()) {

         }

         webSocket.headers()[X_TOKEN]
      }

      fun handleClose(event: Void) {
         closed = true
         if (MAP.get().remove(id) != null) {
            webSocketCounter.decrementAndGet()
         }
      }

      fun handleException(exception: Throwable) {
         closed = true
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

      fun onProtocolError() {

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
   PUSH,
   STREAM,
   PING,
   PONG,
   PRESENCE_GET,
   PRESENCE_LEAVE,
   PRESENCE_STATE,
   PRESENCE_CHANGE,
   PRESENCE_PUSH,
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