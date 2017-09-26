package move.action

import com.google.common.base.Throwables
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.AsyncResult
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import move.NUID
import move.Wire
import org.msgpack.core.MessagePack
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

const val TIMER_TYPE_EVICTION = 1
const val TIMER_TYPE_PING = 2
const val TIMER_TYPE_PONG = 3

data class RemoveServerOptions(
   val port: Int = 15000,
   val host: String = "0.0.0.0",
   val maxFrameSize: Int = RemoteServer.MAX_FRAME,
   val maxMessageSize: Int = RemoteServer.MAX_MESSAGE,
   var maxWebsockets: Int = RemoteServer.MAX_WEBSOCKETS,
   var wsMaxInFlight: Int = RemoteServer.WS_MAX_INFLIGHT,
   var wsMaxStreams: Int = RemoteServer.WS_MAX_STREAMS,
   var pingIntervalSeconds: Long = RemoteServer.PING_INTERVAL_SECONDS,
   var pongTimeoutSeconds: Long = RemoteServer.PONG_TIMEOUT_SECONDS,
   val compression: Boolean = true,
   val decompression: Boolean = true
)

/**
 *
 * @author Clay Molocznik
 */
open class RemoteServer(val port: Int = 15000,
                        val host: String = "0.0.0.0",
                        val maxFrameSize: Int = MAX_FRAME,
                        val maxMessageSize: Int = MAX_MESSAGE,
                        var maxWebsockets: Int = MAX_WEBSOCKETS,
                        var wsMaxInFlight: Int = WS_MAX_INFLIGHT,
                        var wsMaxStreams: Int = WS_MAX_STREAMS,
                        var pingIntervalSeconds: Long = PING_INTERVAL_SECONDS,
                        var pongTimeoutSeconds: Long = PONG_TIMEOUT_SECONDS,
                        val compression: Boolean = true,
                        val compressionLevel: Int = COMPRESSION_LEVEL,
                        val decompression: Boolean = true,
                        val bufferPool: Boolean = true) {
   var httpServer: HttpServer? = null
   val connectionCounter = AtomicLong(0L)
   // Global WebSocket counter.
   val webSocketCounter = AtomicLong(0L)
   var maxWebSockets = MAX_WEBSOCKETS
   var maxInflightPerWebSocket = WS_MAX_INFLIGHT
   val mainRouter = Router.router(VERTX.delegate)
   val httpActionRouter = Router.router(VERTX.delegate)
   val workerActionRouter = Router.router(VERTX.delegate)
   val webSocketRouter = Router.router(VERTX.delegate)

   var auth: JWTAuth = JWTAuth.create(VERTX.delegate, JsonObject())

   val workerMap = Actions
      .worker
      .filter { include(it) }
      .map { it.provider.name to it }
      .toMap()

   protected open fun buildHttpOptions(): HttpServerOptions {
      val options = HttpServerOptions()
         .setPort(port)
         .setUsePooledBuffers(bufferPool)
         .setMaxChunkSize(maxFrameSize)
         .setCompressionLevel(compressionLevel)
         .setCompressionSupported(compression)
         .setDecompressionSupported(decompression)
         .setMaxWebsocketFrameSize(maxFrameSize)
         .setMaxWebsocketMessageSize(maxMessageSize)
         .setHandle100ContinueAutomatically(true)

      if (!host.isBlank()) {
         options.setHost(host)
      }

      return options
   }

   suspend fun start() {
      // Create a WebSocket map for each EventLoop.
      MKernel.initEventLoops { WS_MANAGERS.set(WSManager(it)) }

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
      LOG.info("Closing HTTP Server $host:$port")
      httpServer?.listen()
      awaitEvent<AsyncResult<Void>> { r -> httpServer?.close { r.handle(it) } }
      LOG.info("Closed HTTP Server $host:$port")
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
      // @Http Actions are preferred to this.
      Actions.worker
         .filter { include(it) }
         .filter { !it.guarded }
         .forEach { producer ->
            val annotation = producer.provider.annotation

            var route = if (annotation.path.isBlank())
               router.route()
            else
               router.route(path(annotation.path))

            // Accept POST only.
            route.method(HttpMethod.POST)

            // Consume and produce JSON
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

   protected open fun tokenFor(value: String): ActionToken {
      return NoToken
   }

   suspend protected open fun verifyConnection(value: String): VerifyResult {
      return VerifyResult(
         tokenToPass = tokenFor(value),
         expires = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(24L),
         maxInFlight = wsMaxInFlight,
         maxStreamsPerConnection = wsMaxStreams,
         maxConnections = 10,
         userId = ""
      )
   }

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

   protected open fun catchAll(routingContext: RoutingContext) {
      routingContext
         .response()
         .setStatusCode(HttpResponseStatus.NOT_FOUND.code())
         .end("Not Found")
   }

   protected open fun onWebSocket(webSocket: ServerWebSocket) {
      val manager = WS_MANAGERS.get()

      if (manager == null) {
         val eventLoop = MKernel.nextOrdered()
         eventLoop.execute {
            val _manager = WS_MANAGERS.get()
            async(_manager.eventLoop.dispatcher) {
               _manager.connect(webSocket)
            }
         }
      } else {
         async(manager.eventLoop.dispatcher) {
            manager.connect(webSocket)
         }
      }
   }

   object NeedsToken : ActionToken {
      override val secured = false
   }

   companion object {
      val LOG = LoggerFactory.getLogger(RemoteServer::class.java)
      val MAX_FRAME = 65536
      val MAX_MESSAGE = MAX_FRAME * 64
      val MAX_WEBSOCKETS = 1_000_000
      val WS_MAX_INFLIGHT = 256
      val WS_MAX_STREAMS = 65536
      val PING_INTERVAL_SECONDS = 30L
      val PONG_TIMEOUT_SECONDS = 5L
      val APPLICATION_JSON = "application/json"
      val APPLICATION_MSGPACK = "application/x-msgpack"
      val MAX_TYPE_LENGTH = 1024
      val MAX_HEADERS_LENGTH = 8192
      val X_TOKEN = "X-Token"
      val COMPRESSION_LEVEL = 6

      val WS_MANAGERS = ThreadLocal<WSManager>()
      val PING_BUFFER = ByteArray(1, { REMOTING_PING })
      val PONG_BUFFER = ByteArray(1, { REMOTING_PONG })
   }

   inner class WSManager(val eventLoop: MEventLoop) {
      val id = NUID.nextGlobal()
      var counter = 0L
      val map = LongHashMap<WS>()

      val NAME_BUFFER = ByteArray(MAX_TYPE_LENGTH)

      suspend fun connect(webSocket: ServerWebSocket) {
         val ws = WS(
            id = ++counter,
            webSocket = webSocket,
            eventLoop = eventLoop
         )
         map.put(ws.id, ws)
         connectionCounter.incrementAndGet()
      }

      inner class WS(val id: Long,
                     val webSocket: ServerWebSocket,
                     val eventLoop: MEventLoop) : HasTimers {
         var token: ActionToken = NeedsToken

         // In-Flight counter.
         var inFlight = 0
         var latestId = -1L

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
         var totalLatency = 0L
         var minLatency = Int.MAX_VALUE
         var avgLatency = -1
         var maxLatency = Int.MIN_VALUE

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

         private fun schedulePing(waitSeconds: Long = pingIntervalSeconds) {
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
               TimeUnit.SECONDS.toMillis(pongTimeoutSeconds)
            )
         }

         private fun evict() {
            // Ensure WebSocket is closed.
            webSocket.close()
         }

         private fun sendPing() {
            try {
               pings++
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
               val latency = (System.currentTimeMillis() - pingEpoch).toInt()
               totalLatency += System.currentTimeMillis() - pingEpoch
               maxLatency = Math.max(latency, maxLatency)
               minLatency = Math.min(latency, minLatency)
               avgLatency = (latency / pings).toInt()
            } finally {
               schedulePing()
            }
         }

         private fun onPongTimeout() {
            if (LOG.isDebugEnabled)
               LOG.debug("Timed-out waiting for PONG. Closing WebSocket...")

            webSocket.close()
         }

         fun forceClose() {
            closed = true
            if (map.remove(id) != null) {
               webSocketCounter.decrementAndGet()
               closedFinished = true
            }
         }

         fun handleClose(event: Void) {
            forceClose()
         }

         fun handleException(exception: Throwable) {
            LOG.debug("WebSocket had an exception", exception)
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

         fun handleBinaryConnect(buffer: Buffer) {
            try {
               val buf = buffer.byteBuf

               val unpacker = MessagePack.newDefaultUnpacker(buf.nioBuffer())

               val type = unpacker.unpackByte()

               if (type != REMOTING_MSG_CONNECT) {
                  onBinaryProtocolError(REMOTING_MSG_PROTOCOL_EXPECT_CONNECT)
                  return
               }
            } catch (e: Throwable) {

            }
         }

         fun handleBinary(buffer: Buffer) {
            try {
               val buf = buffer.byteBuf

               if (!buf.isReadable) {
                  return
               }

               val unpacker = MessagePack
                  .newDefaultUnpacker(
                     ByteBufInputStream(buf, true)
                  )

               val messageType = unpacker.unpackByte()

               try {

                  when (messageType) {
                     REMOTING_MSG_ASK_REPLY, REMOTING_MSG_ASK_ACK, REMOTING_MSG_ASK_ACK_AND_REPLY, REMOTING_MSG_ASK_PUSH -> {
                        val requestId = unpacker.unpackLong()

                        if (requestId <= latestId) {
                           onBinaryProtocolError(REMOTING_MSG_REQUEST_ID_REUSED)
                           return
                        }

                        val typeLength = unpacker.unpackInt()

                        if (typeLength > MAX_TYPE_LENGTH) {
                           onBinaryProtocolError(REMOTING_MSG_TYPE_LENGTH_EXCEEDED)
                           return
                        }

                        // Read into Buffer.
                        unpacker.readPayload(NAME_BUFFER)

                        // Create a Zero-Copy ActionName.
                        val actionName = ActionName(NAME_BUFFER, 0, typeLength, false)

                        // Find Producer.
                        val producer = workerMap[actionName] ?: // Return REMOTING_MSG_ERROR_NOT_FOUND
                           return

                        // Read timeout.
                        val timeoutSeconds = unpacker.unpackInt()

                        // -1 = Try to have No Time Limit
                        // 0 = Action default
                        // >0 = Use timeout.

                        // Increment In-Flight.
                        incInFlight()
                        val deferred = producer.launchFromMsgPack(
                           buf,
                           token,
                           timeoutSeconds.toLong(),
                           TimeUnit.SECONDS
                        )

                        deferred.invokeOnCompletion { exception ->
                           if (MKernel.currentEventLoop !== eventLoop) {
                              eventLoop.execute {
                                 // Decrement In-Flight.
                                 decInFlight()

                                 // Ignore Reply if client doesn't care about it.
                                 if (messageType == REMOTING_MSG_ASK_PUSH) {
                                    return@execute
                                 }

                                 if (exception != null) {
                                    if (exception is ActionTimeoutException) {
                                       sendBinaryAskError(
                                          REMOTING_MSG_ERROR_TIMEOUT,
                                          requestId,
                                          ""
                                       )
                                    } else {
                                       // Return internal error.
                                       sendBinaryAskError(
                                          REMOTING_MSG_ERROR_INTERNAL,
                                          requestId,
                                          exception.localizedMessage
                                       )
                                    }
                                 } else {
                                    sendBinaryAskReply(requestId, deferred.getCompleted())
                                 }
                              }
                           } else {
                              // Decrement In-Flight.
                              decInFlight()

                              // Ignore Reply if client doesn't care about it.
                              if (messageType == REMOTING_MSG_ASK_PUSH) {
                                 return@invokeOnCompletion
                              }

                              if (exception != null) {
                                 if (exception is ActionTimeoutException) {
                                    sendBinaryAskError(
                                       REMOTING_MSG_ERROR_TIMEOUT,
                                       requestId,
                                       ""
                                    )
                                 } else {
                                    // Return internal error.
                                    sendBinaryAskError(
                                       REMOTING_MSG_ERROR_INTERNAL,
                                       requestId,
                                       exception.localizedMessage
                                    )
                                 }
                              } else {
                                 sendBinaryAskReply(requestId, deferred.getCompleted())
                              }
                           }
                        }
                     }

                     REMOTING_MSG_ACK, REMOTING_MSG_REPLY, REMOTING_MSG_PUSH, REMOTING_MSG_PUSH_ASK -> {
                        // Ignore. For Client!
                     }

                     REMOTING_PING -> {
                        // Send PONG
                        webSocket.writeFinalBinaryFrame(Buffer.buffer(PONG_BUFFER))
                     }

                     REMOTING_PONG -> {
                        receivePong()
                     }
                     else -> {
                     }
                  }
               } catch (e: Throwable) {
                  onBinaryProtocolError(REMOTING_MSG_PROTOCOL_INCOMPLETE, "Message was incomplete")
               } finally {
                  unpacker.close()
               }
            } catch (e: Throwable) {

            }
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

         private fun sendTextAskError(error: Byte, requestId: Long, msg: String) {
            webSocket.writeFinalTextFrame("$error $requestId $msg")
         }

         fun onTextProtocolError(code: Byte, msg: String = "") {
            webSocket.writeFinalTextFrame("$code $msg")
            webSocket.close()
         }

         private fun sendBinaryAskError(error: Byte, requestId: Long, msg: String) {
            val packer = MessagePack.newDefaultBufferPacker()
            packer.packByte(REMOTING_MSG_ERROR_ASK)
            packer.packLong(requestId)
            packer.packByte(error)
            if (!msg.isEmpty()) {
               packer.packString(msg)
            }

            try {
               webSocket.writeFinalBinaryFrame(Buffer.buffer(packer.toByteArray()))
            } finally {
               packer.close()
            }
         }

         private fun sendBinaryAskReply(requestId: Long, payload: Any) {
            val packer = MessagePack.newDefaultBufferPacker()

            try {
               packer.packByte(REMOTING_MSG_REPLY)
               packer.packLong(requestId)

               if (payload !is ByteArray) {
                  val b = Wire.pack(payload)
                  packer.packInt(b.size)
                  packer.writePayload(b)
               } else {
                  if (payload === Unit) {
                     packer.packInt(0)
                  } else {
                     packer.packInt(payload.size)
                     packer.writePayload(payload)
                  }
               }

               webSocket.writeFinalBinaryFrame(Buffer.buffer(packer.toByteArray()))
            } finally {
               packer.close()
            }
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

         fun onBinaryProtocolError(code: Byte, msg: String = "") {
            val packer = MessagePack.newDefaultBufferPacker()

            try {
               packer.packByte(code)
               packer.packString(msg)
               webSocket.writeFinalBinaryFrame(Buffer.buffer(packer.toByteArray()))
               webSocket.close()
            } finally {
               packer.close()
            }
         }
      }
   }
}

object RejectedVerifyResult : VerifyResult(NoToken, 0)

open class VerifyResult(
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


private const val INVALID: Byte = -1

// Connect
private const val REMOTING_MSG_CONNECT: Byte = 0
private const val REMOTING_MSG_CONNECTED: Byte = 1

// Ping-Pong
private const val REMOTING_PING: Byte = 2
private const val REMOTING_PONG: Byte = 3

// Ask Upstream Messages
private const val REMOTING_MSG_ASK_REPLY: Byte = 4
private const val REMOTING_MSG_ASK_ACK: Byte = 5
private const val REMOTING_MSG_ASK_ACK_AND_REPLY: Byte = 6
private const val REMOTING_MSG_ASK_PUSH: Byte = 7

// Ask Downstream Messages
private const val REMOTING_MSG_ACK: Byte = 8 // ACK message for an ASK - Final if ASK_ACK
private const val REMOTING_MSG_REPLY: Byte = 9  // Reply to ASK - Final
private const val REMOTING_MSG_PUSH_ASK: Byte = 10 // Optional ASK updates


// Ask Error
private const val REMOTING_MSG_ERROR_ASK: Byte = 40
private const val REMOTING_MSG_ERROR_NOT_FOUND: Byte = 41
private const val REMOTING_MSG_ERROR_TIMEOUT: Byte = 42
private const val REMOTING_MSG_ERROR_INTERNAL: Byte = 43
private const val REMOTING_MSG_REQUEST_ID_REUSED: Byte = 44
private const val REMOTING_MSG_TYPE_LENGTH_EXCEEDED: Byte = 45


// Push
private const val REMOTING_MSG_PUSH: Byte = 15

// Streams
private const val REMOTING_MSG_STREAM_OPENED: Byte = 20
private const val REMOTING_MSG_STREAM_PING: Byte = 21
private const val REMOTING_MSG_STREAM_PONG: Byte = 22
private const val REMOTING_MSG_STREAM_CLOSE: Byte = 23
private const val REMOTING_MSG_STREAM_CLOSED: Byte = 24
private const val REMOTING_MSG_STREAM_FRAME: Byte = 25
private const val REMOTING_MSG_STREAM_PAUSE: Byte = 26
private const val REMOTING_MSG_STREAM_RESUME: Byte = 27

// Presence
private const val REMOTING_MSG_PRESENCE_LEAVE: Byte = 30
private const val REMOTING_MSG_PRESENCE_GET: Byte = 31
private const val REMOTING_MSG_PRESENCE_UPDATE_STATUS: Byte = 32
private const val REMOTING_MSG_PRESENCE_CHANGE: Byte = 33 // Multiple Changes
private const val REMOTING_MSG_PRESENCE_CHANGE_JOIN: Byte = 34 // Single Join
private const val PRESENCE_CHANGE_REMOVED: Byte = 35 // Single Remove


// Token
private const val REMOTING_MSG_UPDATE_TOKEN: Byte = 50
private const val REMOTING_MSG_ERROR_TOKEN_EXPIRED: Byte = 51
private const val REMOTING_MSG_ERROR_TOKEN_TOO_MANY_CONNECTIONS: Byte = 52
private const val REMOTING_MSG_INFO_TOKEN_EXPIRING_SOON: Byte = 52

// Close
private const val REMOTING_MSG_CLOSE: Byte = 60
private const val REMOTING_MSG_CLOSE_EXPIRED: Byte = 61
private const val REMOTING_MSG_CLOSE_INACTIVE: Byte = 62
private const val REMOTING_MSG_CONNECTION_ERROR: Byte = 64

// Protocol Error
private const val REMOTING_MSG_PROTOCOL_EXPECT_CONNECT: Byte = 80
private const val REMOTING_MSG_PROTOCOL_INCOMPLETE: Byte = 81
private const val REMOTING_MSG_PROTOCOL_INVALID_TYPE: Byte = 82
private const val REMOTING_MSG_PROTOCOL_TYPE_RESERVED_FOR_SERVER: Byte = 83

// Tell the client to open a new connection
// and to stop sending requests through this one
// since the server wants to shutdown or a re-balance is
// happening.
private const val REMOTING_MSG_INITIATE_HANDOFF: Byte = 90


// [Encoding] [Compression] [TokenLength][Token]
data class Connect(
   val format: Byte,
   val compression: Byte,
   val token: String
)

/**
 *
 */
data class Connected(
   val version: String,
   val expires: Long,
   val maxInFlight: Int,
   val maxFrameSize: Int,
   val maxMessageSize: Int,
   val hasPresence: Boolean,
   val maxPresence: Int,
   val hasStreams: Boolean,
   val maxStreams: Int,
   val compression: Byte
)

const val REMOTING_FORMAT_JSON: Byte = 1
const val REMOTING_FORMAT_CBOR: Byte = 2
const val REMOTING_FORMAT_MSGPACK: Byte = 3

const val REMOTING_COMPRESSION_LZ4: Byte = 1
const val REMOTING_COMPRESSION_GZIP: Byte = 2
const val REMOTING_COMPRESSION_DEFLATE: Byte = 3

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
   /**
    *
    */
   val id: Long,
   /**
    *
    */
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

/**
 *
 */
data class StreamFrameMsg(val id: Long, val payload: ByteBuf)

/**
 *
 */
data class Presence(
   val id: String,
   val created: Long,
   var mod: Long,
   val members: List<PresenceMember>
)

/**
 *
 */
data class PresenceMember(
   val id: String,
   val joined: Long,
   val name: String,
   val image: String,
   val status: String,
   val statusChanged: Long
)

/**
 *
 */
data class PresenceStatus(
   val mod: Long,
   val memberId: String,
   val status: String
)

/**
 *
 */
data class PresenceJoin(
   val mod: Long,
   val member: PresenceMember
)

/**
 *
 */
data class PresenceRemoved(
   val mod: Long,
   val memberId: String
)

/**
 *
 */
data class PresenceChanged(
   val mod: Long,
   val joined: List<PresenceMember>,
   val removed: List<String>,
   val status: List<PresenceChangedStatus>
)

/**
 *
 */
data class PresenceChangedStatus(
   val memberId: String,
   val status: String
)