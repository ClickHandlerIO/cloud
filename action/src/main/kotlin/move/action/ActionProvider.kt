package move.action

import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.rxjava.core.Vertx
import org.HdrHistogram.ActionHistogram
import org.HdrHistogram.Histogram
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.LongAdder
import java.util.function.Function
import javax.inject.Inject
import javax.inject.Provider
import kotlin.reflect.KProperty

/**
 * Builds and invokes a single type of Action.

 * @author Clay Molocznik
 */
abstract class ActionProvider<A : Action<RoutingContext, OUT>, RoutingContext : Any, OUT : Any>
constructor(val vertx: Vertx, val actionProvider: Provider<A>) {
   val actionProviderClass = TypeResolver
      .resolveRawClass(
         ActionProvider::class.java,
         javaClass
      )

   @Suppress("UNCHECKED_CAST")
   val actionClass = TypeResolver
      .resolveRawArgument(
         actionProviderClass.typeParameters[0],
         javaClass
      ) as Class<A>

   @Suppress("UNCHECKED_CAST")
   val requestClass = TypeResolver
      .resolveRawArgument(
         actionProviderClass.typeParameters[1],
         javaClass
      ) as Class<RoutingContext>

   @Suppress("UNCHECKED_CAST")
   val replyClass = TypeResolver.resolveRawArgument(
      actionProviderClass.typeParameters[2],
      javaClass
   ) as Class<OUT>

   val vertxCore: io.vertx.core.Vertx = vertx.delegate
   val eventLoopGroup = MoveEventLoopGroup.get(vertx)

   open val isInternal = false
   open val isWorker = false
   open val isHttp = false
   open val isDaemon = false

   var name: String = findName("")
      get
      internal set

   var broker: ActionBroker by BrokerDelegate()

   class BrokerDelegate {
      var value = ActionBroker.DEFAULT

      operator fun getValue(thisRef: Any?, property: KProperty<*>): ActionBroker {
         return value
      }

      operator fun setValue(thisRef: Any?, property: KProperty<*>, value: ActionBroker) {
         this.value = value
      }
   }


   init {
      init()
   }

   fun findName(fromAnnotation: String?): String {
      if (fromAnnotation?.isNotBlank() == true) {
         return fromAnnotation.trim()
      }
      val n = actionClass.canonicalName
      if (n.startsWith("action.")) {
         return n.substring("action.".length)
      }
      return n
   }

   protected open fun init() {
   }

   companion object {
      private val NOOP: Handler<Void> = Handler { }
      internal val MRoutingContext_TIMEOUT_MILLIS = 200
      internal val MILLIS_TO_SECONDS_THRESHOLD = 500
      val CIRCUIT_BREAKER_RESET_TIMEOUT = 2000L
   }


   internal var maxFailures: Long = Long.MAX_VALUE
   internal val passed = AtomicInteger()
   private var openHandler: Handler<Void> = NOOP
   private var halfOpenHandler: Handler<Void> = NOOP
   private var closeHandler: Handler<Void> = NOOP
   private var fallback: Function<*, *>? = null
   private @Volatile
   var state = CircuitBreakerState.CLOSED
   private var checkFailuresTask: Long = -1L
   private val activeFailures = AtomicLong(0L)

   @Synchronized
   fun openHandler(handler: Handler<Void>) {
      Objects.requireNonNull(handler)
      openHandler = handler
   }

   @Synchronized
   fun halfOpenHandler(handler: Handler<Void>) {
      Objects.requireNonNull(handler)
      halfOpenHandler = handler
   }

   @Synchronized
   fun closeHandler(handler: Handler<Void>) {
      Objects.requireNonNull(handler)
      closeHandler = handler
   }

   fun <T> fallback(handler: Function<Throwable, T>) {
      Objects.requireNonNull(handler)
      fallback = handler
   }

   //   @Synchronized
   fun reset() {
      activeFailures.set(0L)

      if (state == CircuitBreakerState.CLOSED) {
         // Do nothing else.
         return
      }

      state = CircuitBreakerState.CLOSED
      closeHandler.handle(null)
   }

   @Synchronized
   internal fun open() {
      if (state == CircuitBreakerState.OPEN) {
         return
      }

      state = CircuitBreakerState.OPEN
      openHandler.handle(null)

      // Set up the attempt reset timer
      val period = CIRCUIT_BREAKER_RESET_TIMEOUT
      if (period != -1L) {
         vertx.setTimer(period) { l -> attemptReset() }
      }
   }

   fun failureCount(): Long {
      return activeFailures.get()
   }

   fun state(): CircuitBreakerState {
      return state
   }

   private fun attemptReset() {
      if (state == CircuitBreakerState.OPEN) {
         passed.set(0)
         state = CircuitBreakerState.HALF_OPEN
         halfOpenHandler.handle(null)
      }
   }

   fun name(): String {
      return name
   }

   internal fun incrementFailures() {
      activeFailures.incrementAndGet()
   }

   private fun checkFailures() {
      if (activeFailures.get() >= maxFailures) {
         if (state != CircuitBreakerState.OPEN) {
            open()
         }
      }
   }


   private val node: String = ""
   private var circuitBreakerResetTimeout: Long = CIRCUIT_BREAKER_RESET_TIMEOUT

   // Global statistics
   private val metricsCount = AtomicInteger(0)
   internal val durationMs = LongAdder()
   internal val cpuTime = LongAdder()
   internal val blocking = LongAdder()
   internal val failures = LongAdder()
   internal val success = LongAdder()
   internal val timeout = LongAdder()
   internal val exceptions = LongAdder()
   //  private Histogram statistics = new ConcurrentHistogram(3);
   //  private Histogram statistics = new ConcurrentHistogram(3);
   internal val statistics = ActionHistogram(60000, 3)
   //  private Histogram cachedRolling1 = new ConcurrentHistogram(3);
   internal val cachedRolling1 = ActionHistogram(60000, 3)
   //  private Histogram cachedRolling2 = new ConcurrentHistogram(3);
   internal val cachedRolling2 = ActionHistogram(60000, 3)
   @Volatile private var currentWindow = RollingWindow()

//   internal fun complete(operation: Operation) {
      //    final RollingWindow window = this.currentWindow;

      //    final long durationInMs = operation.durationInNanos();
      //    this.durationMs.add(durationInMs);

      // Compute global statistics
      //    statistics.recordValue(operation.durationInNanos());

      //    cpuTime.add(operation.cpu);
      //    window.cpuTime.add(operation.cpu);

      //    if (operation.blocking > 0L) {
      //      blocking.add(operation.blocking);
      ////      window.blocking.add(operation.blocking);
      //    }

      //    window.operationDurationMs.add(durationInMs);
      //    window.stats.recordValue(durationInMs);
      //    if (operation.exception) {
      //      exceptions.increment();
      ////      window.exception.increment();
      //    } else if (operation.complete) {
      //      success.increment();
      ////      window.success.increment();
      //    } else if (operation.timeout) {
      //      timeout.increment();
      ////      window.timeout.increment();
      //    } else if (operation.failed) {
      //      failures.increment();
      ////      window.failure.increment();
      //    }
      //
      //    if (operation.fallbackSucceed) {
      ////      window.fallbackSuccess.increment();
      //    } else if (operation.fallbackFailed) {
      ////      window.fallbackFailure.increment();
      //    }
      //
      //    if (operation.shortCircuited) {
      ////      window.shortCircuited.increment();
      //    }
//   }

//   @Synchronized
//   fun toJson(): JsonObject {
//      val json = JsonObject()
//
//      metricsCount.incrementAndGet()
//      val window = this.currentWindow
//      val rollingStatistics = window.stats.copy()
//      this.currentWindow = RollingWindow()
//      val statistics = this.statistics.copy()
//      val end = System.currentTimeMillis()
//
//      val calls = statistics.totalCount
//      val success = this.success.sum()
//      val failures = this.failures.sum()
//      val exceptions = this.exceptions.sum()
//      val timeout = this.timeout.sum()
//      val cpu = this.cpuTime.sum()
//      val blocking = this.blocking.sum()
//
//      // Configuration
//      json.put("begin", rollingStatistics.startTimeStamp)
//      json.put("duration", end - rollingStatistics.endTimeStamp)
//      json.put("resetTimeout", circuitBreakerResetTimeout)
//      json.put("deadline", circuitBreakerTimeout)
//      json.put("metricRollingWindow", rollingWindow)
//      json.put("name", name)
//      json.put("node", node)
//
//      // Current state
//      json.put("state", state)
//      json.put("failures", failureCount())
//
//      // Global metrics
//      json.put("totalErrorCount", failures + exceptions + timeout)
//      json.put("totalSuccessCount", success)
//      json.put("totalTimeoutCount", timeout)
//      json.put("totalExceptionCount", exceptions)
//      json.put("totalFailureCount", failures)
//      json.put("totalOperationCount", calls)
//      json.put("totalCpu", cpu)
//      json.put("totalBlocking", blocking)
//      if (calls == 0) {
//         json.put("totalSuccessPercentage", 0)
//         json.put("totalErrorPercentage", 0)
//      } else {
//         json.put("totalSuccessPercentage", success.toDouble() / calls * 100)
//         json.put("totalErrorPercentage", (failures + exceptions + timeout).toDouble() / calls * 100)
//      }
//
//      addLatency(json, statistics, "total")
//
//      val rollingOperations = rollingStatistics.totalCount
//      val rollingException = window.exception.sum()
//      val rollingFailure = window.failure.sum()
//      val rollingSuccess = window.success.sum()
//      val rollingTimeout = window.timeout.sum()
//      val rollingCPU = window.cpuTime.sum()
//      val rollingFallbackSuccess = window.fallbackSuccess.sum()
//      val rollingFallbackFailure = window.fallbackFailure.sum()
//      val rollingShortCircuited = window.shortCircuited.sum()
//      val rollingBlocking = window.blocking.sum()
//
//      json.put("rollingOperationCount", rollingOperations - rollingShortCircuited)
//      json.put("rollingErrorCount", rollingException + rollingFailure + rollingTimeout)
//      json.put("rollingSuccessCount", rollingSuccess)
//      json.put("rollingTimeoutCount", rollingTimeout)
//      json.put("rollingExceptionCount", rollingException)
//      json.put("rollingFailureCount", rollingFailure)
//      json.put("rollingCpu", rollingCPU)
//      json.put("rollingBlocking", rollingBlocking)
//      if (rollingOperations == 0) {
//         json.put("rollingSuccessPercentage", 0)
//         json.put("rollingErrorPercentage", 0)
//      } else {
//         json.put("rollingSuccessPercentage", rollingSuccess.toDouble() / rollingOperations * 100)
//         json.put("rollingErrorPercentage",
//            (rollingException + rollingFailure + rollingTimeout + rollingShortCircuited).toDouble() / rollingOperations * 100)
//      }
//
//      json.put("rollingFallbackSuccessCount", rollingFallbackSuccess)
//      json.put("rollingFallbackFailureCount", rollingFallbackFailure)
//      json.put("rollingShortCircuitedCount", rollingShortCircuited)
//
//      addLatency(json, rollingStatistics, "rolling")
//      return json
//   }

   private fun addLatency(json: JsonObject, histogram: Histogram, prefix: String) {
      json.put(prefix + "LatencyMean", histogram.mean)
      json.put(prefix + "Latency", JsonObject()
         .put("0", histogram.getValueAtPercentile(0.0))
         .put("25", histogram.getValueAtPercentile(25.0))
         .put("50", histogram.getValueAtPercentile(50.0))
         .put("75", histogram.getValueAtPercentile(75.0))
         .put("90", histogram.getValueAtPercentile(90.0))
         .put("95", histogram.getValueAtPercentile(95.0))
         .put("99", histogram.getValueAtPercentile(99.0))
         .put("99.5", histogram.getValueAtPercentile(99.5))
         .put("100", histogram.getValueAtPercentile(100.0)))
   }

   private fun nextRollingHistogram(): Histogram {
      val histogram = if (metricsCount.get() % 2 == 0)
         cachedRolling1
      else
         cachedRolling2

      histogram.reset()

      return histogram
   }

   private inner class RollingWindow {

      internal var stats = nextRollingHistogram()
      internal var operationDurationMs = LongAdder()
      internal var cpuTime = LongAdder()
      internal var blocking = LongAdder()
      internal var exception = LongAdder()
      internal var failure = LongAdder()
      internal var success = LongAdder()
      internal var timeout = LongAdder()
      internal var fallbackSuccess = LongAdder()
      internal var fallbackFailure = LongAdder()
      internal var shortCircuited = LongAdder()
   }

//   internal inner class Operation {
//
//      var begin: Long = 0
//      var cpu: Long = 0
//      var cpuBegin: Long = 0
//      var blocking: Long = 0
//      var blockingBegin: Long = 0
//      var child: Long = 0
//      var childCpu: Long = 0
//      var childBlocking: Long = 0
//
//      fun begin() {
//         begin = System.nanoTime()
//      }
//
//      fun cpuStart() {
//         cpuBegin = System.nanoTime()
//      }
//
//      fun cpuEnd(): Long {
//         val increaseBy = System.nanoTime() - cpuBegin
//         cpu += increaseBy
//         this@ActionProvider.cpuTime.add(increaseBy)
//         return increaseBy
//      }
//
//      @Synchronized
//      fun blockingBegin() {
//         blockingBegin = System.nanoTime()
//      }
//
//      @Synchronized
//      fun blockingEnd() {
//         blocking += System.nanoTime() - blockingBegin
//         this@ActionProvider.blocking.add(blocking)
//      }
//
//      fun complete() {
//         this@ActionProvider.success.increment()
//         val duration = (System.nanoTime() - begin) / 1000000
//         this@ActionProvider.durationMs.add(duration)
//         cachedRolling1.recordValue(duration)
//         //      cachedRolling1.recordValue(duration);
//      }
//
//      fun failed() {
//         failures.increment()
//         val duration = (System.nanoTime() - begin) / 1000000
//         this@ActionProvider.durationMs.add(duration)
//      }
//
//      fun timeout() {
//         this@ActionProvider.timeout.increment()
//         val duration = (System.nanoTime() - begin) / 1000000
//         this@ActionProvider.durationMs.add(duration)
//      }
//
//      fun error() {
//         this@ActionProvider.exceptions.increment()
//         val duration = (System.nanoTime() - begin) / 1000000
//         this@ActionProvider.durationMs.add(duration)
//      }
//
//      fun fallbackFailed() {
//
//      }
//
//      fun fallbackSucceed() {
//
//      }
//
//      fun shortCircuited() {
//         val duration = (System.nanoTime() - begin) / 1000000
//         this@ActionProvider.durationMs.add(duration)
//      }
//   }
}

/**
 *
 */
class CircuitOpenException : RuntimeException()








/**

 */
open class InternalActionProvider<A : InternalAction<RoutingContext, OUT>, RoutingContext : Any, OUT : Any> @Inject
constructor(vertx: Vertx,
            actionProvider: Provider<A>) : ActionProvider<A, RoutingContext, OUT>(
   vertx, actionProvider
) {
   override val isInternal = true

   val annotation: Internal? = actionClass.getAnnotation(Internal::class.java)

   @Suppress("UNCHECKED_CAST")
   val self = this as InternalActionProvider<InternalAction<RoutingContext, OUT>, RoutingContext, OUT>

   val annotationTimeout: Int
      get() = annotation?.timeout ?: 0

   private var executionTimeoutEnabled: Boolean = false
   internal var timeoutMillis: Int = annotationTimeout
   internal var timeoutMillisLong = timeoutMillis.toLong()

   // Calculate the default number of Ticks to constitute a timeout.
   internal var timeoutTicks = if (timeoutMillisLong > 0)
      (timeoutMillisLong / MoveEventLoop.TICK_MS).let { if (it < 1) 1 else it}
   else
      0

   var maxConcurrentRequests: Int = 0
      internal set

   var isExecutionTimeoutEnabled: Boolean
      get() = executionTimeoutEnabled
      set(enabled) {
      }


   override fun init() {
   }


//   protected open fun calcTimeout(timeoutMillis: Long, context: IActionContext): Long {
//      if (timeoutMillis < 1) {
//         return context.deadline
//      }
//
//      // Calculate max execution millis.
//      val deadline = System.currentTimeMillis() + timeoutMillis
//      if (deadline > context.deadline) {
//         return context.deadline
//      }
//
//      return deadline
//   }

   protected open fun deadline(timeoutMillis: Long): Long {
      // Force No-Timeout.
      if (timeoutMillis < 0)
         return 0L

      // Use default timeout.
      if (timeoutMillis == 0L) {
         return if (timeoutMillisLong > 0L)
            System.currentTimeMillis() + timeoutMillisLong
         else
            0
      }

      // Calculate deadline.
      return System.currentTimeMillis() + timeoutMillis
   }
}





/**

 */
open class WorkerActionProvider<A : WorkerAction<RoutingContext, OUT>, RoutingContext : Any, OUT : Any> @Inject
constructor(vertx: Vertx,
            actionProvider: Provider<A>) : ActionProvider<A, RoutingContext, OUT>(vertx, actionProvider) {

   override val isWorker = true

   val annotation: Worker? = actionClass.getAnnotation(Worker::class.java)
   val annotationTimeout = annotation?.timeout ?: 0

   @Suppress("UNCHECKED_CAST")
   val self = this as WorkerActionProvider<WorkerAction<RoutingContext, OUT>, RoutingContext, OUT>

   val visibility: ActionVisibility
      get() = annotation?.visibility ?: ActionVisibility.PRIVATE

   private var executionTimeoutEnabled: Boolean = false
   internal var timeoutMillis: Int = annotationTimeout
   internal var timeoutMillisLong = timeoutMillis.toLong()

   var maxConcurrentRequests: Int = 0
      internal set

   var isExecutionTimeoutEnabled: Boolean
      get() = executionTimeoutEnabled
      set(enabled) {
      }

   private var queueGroup: String = ""

   val isFifo = annotation?.fifo ?: false
   val queueName = name?.replace(".", "-")?.replace("action-worker", "") + if (isFifo) ".fifo" else ""

   internal var producer: WorkerProducer? = null

   override fun init() {
      name = findName(annotation?.value)
      queueGroup = name

      // Timeout milliseconds.
      var timeoutMillis = 0
      if (this.timeoutMillis > 0L) {
         timeoutMillis = this.timeoutMillis
      }

      this.timeoutMillis = timeoutMillis

      // Enable deadline?
      if (timeoutMillis > 0) {
         if (timeoutMillis < MILLIS_TO_SECONDS_THRESHOLD) {
            // Looks like somebody decided to put seconds instead of milliseconds.
            timeoutMillis = timeoutMillis * 1000
         } else if (timeoutMillis < 1000) {
            timeoutMillis = 1000
         }

         this.timeoutMillis = timeoutMillis
         executionTimeoutEnabled = true
      }
   }

   protected open fun deadline(timeoutMillis: Long): Long {
      // Force No-Timeout.
      if (timeoutMillis < 0)
         return 0L

      // Use default timeout.
      if (timeoutMillis == 0L) {
         return if (timeoutMillisLong > 0L)
            System.currentTimeMillis() + timeoutMillisLong
         else
            0
      }

      // Calculate deadline.
      return System.currentTimeMillis() + timeoutMillis
   }
}

/**
 *
 */
class WorkerReceipt @Inject constructor() {
   var mD5OfMessageBody: String? = null
   var mD5OfMessageAttributes: String? = null
   var messageId: String? = null
   /**
    * <p>
    * This parameter applies only to FIFO (first-in-first-out) queues.
    * </p>
    * <p>
    * A large, non-consecutive number that Amazon SQS assigns to each message.
    * </p>
    * <p>
    * The length of <code>SequenceNumber</code> is 128 bits. <code>SequenceNumber</code> continues to increase for a
    * particular <code>MessageGroupId</code>.
    * </p>
    */
   var sequenceNumber: String? = null

   /**
    *
    */
   val isSuccess
      get() = !messageId.isNullOrBlank()
}





open class HttpActionProvider<A : HttpAction>
@Inject constructor(vertx: Vertx, provider: Provider<A>)
   : ActionProvider<A, RoutingContext, Unit>(vertx, provider) {
   override val isHttp = true

   val annotation: Http? = actionClass.getAnnotation(Http::class.java)
   val visibility: ActionVisibility = annotation?.visibility ?: ActionVisibility.PUBLIC

   @Suppress("UNCHECKED_CAST")
   val self = this as HttpActionProvider<HttpAction>

   val annotationTimeout: Int
      get() = annotation?.timeout ?: 0

   private var executionTimeoutEnabled: Boolean = false
   internal var timeoutMillis: Int = annotationTimeout
   internal var timeoutMillisLong = timeoutMillis.toLong()

   var isExecutionTimeoutEnabled: Boolean
      get() = executionTimeoutEnabled
      set(enabled) {
      }

   override fun init() {
      // Timeout milliseconds.
      var timeoutMillis = 0
      if (this.timeoutMillis > 0L) {
         timeoutMillis = this.timeoutMillis
      }

      this.timeoutMillis = timeoutMillis

      // Enable deadline?
      if (timeoutMillis > 0) {
         if (timeoutMillis < MILLIS_TO_SECONDS_THRESHOLD) {
            // Looks like somebody decided to put seconds instead of milliseconds.
            timeoutMillis = timeoutMillis * 1000
         } else if (timeoutMillis < 1000) {
            timeoutMillis = 1000
         }

         this.timeoutMillis = timeoutMillis
         executionTimeoutEnabled = true
      }
   }


   protected open fun deadline(timeoutMillis: Long): Long {
      // Force No-Timeout.
      if (timeoutMillis < 0)
         return 0L

      // Use default timeout.
      if (timeoutMillis == 0L) {
         return if (timeoutMillisLong > 0L)
            System.currentTimeMillis() + timeoutMillisLong
         else
            0
      }

      // Calculate deadline.
      return System.currentTimeMillis() + timeoutMillis
   }
}