package move.action

import com.codahale.metrics.Counter
import com.codahale.metrics.Gauge
import com.google.common.base.Throwables
import com.google.common.util.concurrent.AbstractIdleService
import com.google.common.util.concurrent.Service
import io.vertx.rxjava.core.Vertx
import move.common.Metrics
import move.common.UID
import org.slf4j.LoggerFactory
import rx.Single
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**

 */
@Singleton
class LocalWorkerService @Inject
internal constructor(val vertx: Vertx) : AbstractIdleService(), WorkerService, WorkerProducer {

   val queues = mutableMapOf<String, QueueContext>()

   @Throws(Exception::class)
   override fun startUp() {
      ActionManager.workerActionMap.values.forEach { provider -> provider.producer = this }
      ActionManager.workerActionQueueGroupMap.forEach { entry ->
         // Is Fifo?
         val fifo = entry.value.map { it.isFifo }.first()

         // If there are more than 1 action mapped to this queue then find the max "concurrency"
         val maxParalellism = if (fifo) {
            1
         } else {
            entry.value.map {
               it.concurrency
            }.max()?.toInt() ?: DEFAULT_CONCURRENCY
         }
         // If there are more than 1 action mapped to this queue then find largest "timeoutMillis"
         val maxTimeout = entry.value.map {
            it.timeoutMillis()
         }.max()?.toInt() ?: DEFAULT_WORKER_TIMEOUT_MILLIS

         queues.put(
            entry.key,
            QueueContext(entry.key, maxParalellism)
         )
      }

      // Startup all receivers.
      queues.values.forEach { queueContext ->
         try {
            queueContext.startAsync().awaitRunning()
         } catch (e: Throwable) {
            LOG.error("Failed to start LocalWorkerService.QueueContext for '" + queueContext.queueName + "'")
            throw RuntimeException(e)
         }
      }
   }

   @Throws(Exception::class)
   override fun shutDown() {
      // Startup all receivers.
      queues.values.forEach { queueContext ->
         try {
            queueContext.stopAsync().awaitTerminated(5, TimeUnit.SECONDS)
         } catch (e: Throwable) {
            LOG.error("Failed to stop LocalWorkerService.QueueContext for '" + queueContext.queueName + "'")
            throw RuntimeException(e)
         }
      }
   }

   override fun send(request: WorkerRequest): Single<WorkerReceipt> {
      val name = request.actionProvider?.queueName ?: return Single.just(WorkerReceipt())
      val queueContext = queues[name] ?: return Single.just(WorkerReceipt())

      return Single.create<WorkerReceipt> { subscriber ->

         if (request.delaySeconds > 0) {
            vertx.setTimer(
               TimeUnit.SECONDS.toMillis(request.delaySeconds.toLong())
            ) { event -> queueContext.add(request) }
         } else {
            queueContext.add(request)
         }

         subscriber.onSuccess(WorkerReceipt().apply {
            messageId = UID.next()
         })
      }
   }

   open inner class QueueContext(val queueName: String, var concurrency: Int = 1) : AbstractIdleService() {
      val registry = Metrics.registry()

      val jobs = ConcurrentLinkedDeque<WorkerRequest>()
      val activeMessages = AtomicInteger(0)

      private val activeMessagesGauge: Gauge<Int> = try {
         registry.register<Gauge<Int>>(
            queueName + "-ACTIVE_MESSAGES",
            Gauge<Int> { activeMessages.get() }
         )
      } catch (e: Throwable) {
         registry.metrics[queueName + "-ACTIVE_MESSAGES"] as Gauge<Int>
      }
      private val concurrencyGauge: Gauge<Int> = try {
         registry.register<Gauge<Int>>(
            queueName + "-CONCURRENCY",
            Gauge<Int> { concurrency }
         )
      } catch (e: Throwable) {
         registry.metrics[queueName + "-CONCURRENCY"] as Gauge<Int>
      }
      private val jobsCounter: Counter = registry.counter(queueName + "-JOBS")
      private val timeoutsCounter: Counter = registry.counter(queueName + "-TIMEOUTS")
      private val completesCounter: Counter = registry.counter(queueName + "-COMPLETES")
      private val inCompletesCounter: Counter = registry.counter(queueName + "-IN_COMPLETES")
      private val exceptionsCounter: Counter = registry.counter(queueName + "-EXCEPTIONS")

      override fun startUp() {
         next()
      }

      override fun shutDown() {
      }

      fun add(request: WorkerRequest) {
         jobs.add(request)
         val ctx = Vertx.currentContext()
         if (ctx == null || !ctx.isEventLoopContext) {
            vertx.runOnContext { next() }
         } else {
            next()
         }
      }

      fun shouldRun(): Boolean = when (state()) {
         Service.State.STARTING, Service.State.NEW, Service.State.RUNNING -> true
         else -> false
      }

      fun next() {
         if (!shouldRun()) {
            return
         }

         if (activeMessages.get() >= concurrency) {
            return
         }

         val job = jobs.poll() ?: return

         jobsCounter.inc()

         val r = job.request

         activeMessages.incrementAndGet()
         val actionProvider = job.actionProvider

         actionProvider?.rx(r)?.subscribe(
            {
               try {
                  activeMessages.decrementAndGet()

                  if (it != null && it) {
                     completesCounter.inc()
                  } else {
                     inCompletesCounter.inc()
                     LOG.error("Job was incomplete. Removing from JobQueue. " + actionProvider.actionClass.canonicalName)
                  }
               } finally {
                  vertx.runOnContext { next() }
               }
            },
            {
               try {
                  activeMessages.decrementAndGet()
                  LOG.error("Action " + actionProvider.actionClass.canonicalName + " threw an exception", it)

                  val rootCause = Throwables.getRootCause(it)
                  if (rootCause is ActionTimeoutException || rootCause is TimeoutException) {
                     timeoutsCounter.inc()
                  } else {
                     exceptionsCounter.inc()
                  }
               } finally {
                  vertx.runOnContext { next() }
               }
            }
         )
      }
   }

   companion object {
      internal val LOG = LoggerFactory.getLogger(LocalWorkerService::class.java)
      private val DEFAULT_CONCURRENCY = Runtime.getRuntime().availableProcessors() * 2
      private val DEFAULT_WORKER_TIMEOUT_MILLIS = 30_000
   }
}
