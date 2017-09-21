package move.action

import com.google.common.base.Throwables
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.Unpooled
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
import kotlinx.coroutines.experimental.launch
import move.Wire
import org.msgpack.core.MessagePack
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
open class RemotingServer(val port: Int = 15000,
                          val host: String = "0.0.0.0",
                          var auth: JWTAuth) {
   var httpServer: HttpServer? = null
   val idCounter = AtomicLong(0L)
   // Global WebSocket counter.
   val webSocketCounter = AtomicLong(0L)
   var maxWebSockets = MAX_WEBSOCKETS
   var maxInflightPerWebSocket = MAX_WEBSOCKET_INFLIGHT
   val mainRouter = Router.router(VERTX.delegate)
   val httpActionRouter = Router.router(VERTX.delegate)
   val workerActionRouter = Router.router(VERTX.delegate)
   val webSocketRouter = Router.router(VERTX.delegate)

   suspend fun start() {
      // Create a WebSocket map for each EventLoop.
      MKernel.initEventLoops { MAP.set(LongSkipListMap()) }

      // Build the vertx HTTP server.
      httpServer = buildHttpServer()

      LOG.info("Binding HTTP Server $host:$port")
      // Wait for listen to finish.
      val listenResult = awaitEvent<AsyncResult<HttpServer>> { r ->
         httpServer?.listen(port) {
            r.handle(it)
         }
      }
      LOG.info("Listening on $host:$port")

      // Throw if failed.
      if (listenResult.failed()) {
         throw listenResult.cause()
      }
   }

   suspend fun stop() {
      awaitEvent<AsyncResult<Void>> { r -> httpServer?.close { r.handle(it) } }
   }

   suspend fun buildHttpServer(): HttpServer {
      // Create router.
      val router = Router.router(VERTX.delegate)

      Actions.http
         .filter { include(it) }
         .forEach { producer ->
            val annotation = producer.provider.annotation

            var route = if (annotation.path.isBlank())
               router.route()
            else
               router.route(path(annotation.path))

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
               producer.rxAsk(request = request).invokeOnCompletion {
                  if (it != null) {
                     onUncaughtException(producer, request, it)
                  } else if (!request.response().ended()) {
                     onIncompleteResponse(producer, request)
                  }
               }
            }
         }

      // Add HTTP handlers for all "unguarded" public workers.
      Actions.worker
         .filter { include(it) }
         .filter { !it.guarded }
         .forEach { producer ->
            val annotation = producer.provider.annotation

            var route = if (annotation.path.isBlank())
               router.route()
            else
               router.route(path(annotation.path))

            route.method(HttpMethod.POST)
            route.produces(APPLICATION_JSON)
            route.consumes(APPLICATION_JSON)

            // Create handler.
            route.handler { request ->
               launch(MKernel.currentEventLoop.dispatcher) {
                  val body = awaitEvent<ByteArray> { c ->
                     request.request().bodyHandler {
                        c.handle(it.bytes)
                     }
                  }

                  try {
                     // Launch and await result.
                     val result = producer.launchFromJson(body, NoToken).await()

                     // Return result.
                     request
                        .response()
                        .setStatusCode(200)
                        .end(
                           Wire.stringify(result)
                        )
                  } catch (e: Throwable) {
                     onUncaughtException(producer, request, e)
                  }
               }
            }
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

   protected open fun path(path: String): String {
      if (path.startsWith("/")) {
         return path
      }
      return "/$path"
   }

   protected open fun include(producer: HttpActionProducer<*, *>) =
      producer.visibleTo(ActionVisibility.PUBLIC)

   protected open fun include(producer: WorkerActionProducer<*, *, *, *>) =
      producer.provider.visibility == ActionVisibility.PUBLIC

   /**
    * Add more handlers.
    */
   suspend protected open fun addHandlers(router: Router) {
      val existingPingHandler = router.routes.find { it.path.equals("/ping") }

      if (existingPingHandler == null) {
         router.get("/ping").handler { it.response().setStatusCode(200).end("pong") }
      }
   }

   protected open fun onIncompleteResponse(producer: HttpActionProducer<*, *>,
                                           context: RoutingContext) {

      LOG.error(
         """
         onIncompleteResponse() for [${producer.provider.actionClass.canonicalName}]
         Returned from HttpAction before calling 'resp.end()'
         "Responding with '501 - Not Handled'
         """.trimIndent()
      )
      context.response().setStatusCode(501).end("Not Handled")
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

   protected open fun onUncaughtException(producer: WorkerActionProducer<*, *, *, *>,
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
      var eventLoop = MKernel.currentEventLoop

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
      MKernel.forEachExecute { evict(it) }
   }

   private fun evict(loop: MEventLoop) {

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
      val APPLICATION_JSON = "application/json"

      val MAP = ThreadLocal<LongSkipListMap<WS>>()

   }

   inner class WS(val id: Long,
                  val webSocket: ServerWebSocket,
                  val eventLoop: MEventLoop) : HasTimers {
      // In-Flight counter.
      var inFlight = 0

      // Expiration
      val expires: Long = TimeUnit.HOURS.toMillis(24)
      var lastActivity = eventLoop.epoch

      // Streams
      var streams: Map<Long, Any> = emptyMap()

      // Presence
      var presence: Map<String, Any> = emptyMap()

      // Whether the entire TCP connection is paused.
      // This can be trigged by max In-Flight being too high.
      var paused = false

      // Timer Handles.
      var expirationTimer: TimerEventHandle? = null
      var pingTimer: TimerEventHandle? = null
      var pongTimer: TimerEventHandle? = null

      // Closed
      var closed = false
      var closedFinished = false

      // Round-trip latency measured by PING-PONG keep-alive.
      var pings = 0L
      var pingEpoch = -1L
      var latency = -1L
      var minLatency = -1L
      var avgLatency = -1L
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
         // Schedule eviction.
         eventLoop.scheduleTimer(
            TIMER_TYPE_EVICTION,
            this,
            expires
         )
      }

      private fun schedulePing(waitSeconds: Long = PING_INTERVAL_SECONDS) {
         // Schedule the next PING.
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

      fun forceClose() {
         closed = true
         if (MAP.get().remove(id) != null) {
            webSocketCounter.decrementAndGet()
            closedFinished = true
         }
      }

      fun handleClose(event: Void) {
         forceClose()
      }

      fun handleException(exception: Throwable) {
         closed = true
         webSocket.close()
      }

      fun handleText(message: String) {
         if (message.isEmpty()) {
            return
         }


         // Unpack message.
         incInFlight()
      }

      fun handleBinary(buffer: Buffer) {
         val buf = buffer.byteBuf

         if (!buf.isReadable) {
            return
         }

         val unpacker = MessagePack
            .newDefaultUnpacker(ByteBufInputStream(buffer.byteBuf))

         val messageType = unpacker.unpackByte()

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
   val tokenToPass: ActionToken?,
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
   val maxConnections: Int = 0,
   /**
    *
    */
   val maxStreamsPerConnection: Int = 128
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

class TextRemoteUnpacker {
   var messageType = RemoteMessageType.INVALID

   fun process(message: ByteArray) {
      val buffer = Unpooled.wrappedBuffer(message)

      messageType = RemoteMessageType.from(buffer.readByte().toInt())
   }
}

class BinaryRemoteUnpacker {

   var messageType = RemoteMessageType.INVALID

   fun process(buffer: ByteBuf) {
      val unpacker = MessagePack
         .newDefaultUnpacker(
            ByteBufInputStream(buffer, true)
         )

      try {
         messageType = RemoteMessageType.from(unpacker.unpackByte().toInt())

         if (messageType == RemoteMessageType.INVALID) {
            // TODO: Fix
            return
         }
      } finally {
         unpacker.close()
      }
   }
}

/**
 *
 */
enum class RemoteMessageType(val code: Int) {
   ASK(1),
   REPLY(2),
   PUSH(3),
   STREAM(4),
   PRESENCE(5),
   PING(6),
   PONG(7),
   INVALID(-1);

   companion object {
      fun from(code: Int) = when (code) {
         1 -> ASK
         2 -> REPLY
         3 -> PUSH
         4 -> STREAM
         5 -> PRESENCE
         6 -> PING
         7 -> PONG
         else -> INVALID
      }
   }
}

/**
 *
 */
enum class PresenceMessageType(val code: Int) {
   JOINED(1),
   LEAVE(2),
   GET(3),
   CHANGE(4),
   INVALID(-1);

   companion object {
      fun from(code: Int) = when (code) {
         1 -> JOINED
         2 -> LEAVE
         3 -> GET
         4 -> CHANGE
         else -> INVALID
      }
   }
}

/**
 *
 */
enum class AskReplyType(val code: Int) {
   /**
    * Default behavior where the client wants to
    * wait for the reply.
    */
   REPLY(1),
   /**
    * Client just wants to ensure the backend received
    * the message.
    */
   ACK(2),
   /**
    * Client wants both an ACK and a REPLY
    */
   ACK_AND_REPLY(3),
   /**
    * Client does not want any reply at all.
    */
   PUSH(4),
   /**
    *
    */
   INVALID(-1);

   companion object {
      fun from(code: Int) = when (code) {
         1 -> REPLY
         2 -> ACK
         3 -> ACK_AND_REPLY
         4 -> PUSH
         else -> INVALID
      }
   }
}

/**
 *
 */
data class AskMsg(
   val id: Long,
   val type: String,
   val timeout: Long,
   val replyType: Int,
   val payload: ByteArray
)

/**
 *
 */
data class AskReplyMsg(
   val id: Long,
   val code: Int,
   val type: Int,
   val payload: ByteArray
)

/**
 * Stream info.
 */
data class StreamInfo(
   // UID of the stream.
   val uid: String,
   // Address to send Stream messages to.
   val address: String
)

/**
 * Streams are simple
 */
data class StreamMsg(
   val id: Long,
   val seq: Long,
   // Payload could be framed to support custom protocols
   val payload: ByteBuf
)

/**
 *
 */
data class StreamPingMsg(val id: Long)

/**
 *
 */
data class StreamPongMsg(val id: Long)

/**
 * Stream must be paused and prevent new messages from coming in.
 */
data class StreamPauseMsg(val id: Long)

/**
 * Stream may be resumed.
 */
data class StreamResumeMsg(val id: Long)

/**
 *
 */
data class StreamCloseMsg(val id: Long)