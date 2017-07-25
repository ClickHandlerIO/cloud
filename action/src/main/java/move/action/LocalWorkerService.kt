package move.action

import com.codahale.metrics.Counter
import com.codahale.metrics.Gauge
import com.google.common.base.Throwables
import com.google.common.util.concurrent.AbstractIdleService
import com.netflix.hystrix.exception.HystrixTimeoutException
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

         // If there are more than 1 action mapped to this queue then find the max "parallelism"
         val maxParalellism = entry.value.map {
            it.parallelism()
         }.max()?.toInt() ?: DEFAULT_PARALELLISM
         // If there are more than 1 action mapped to this queue then find largest "timeoutMillis"
         val maxExecutionMillis = entry.value.map {
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

   open inner class QueueContext(val queueName: String, var parallelism: Int = 1) : AbstractIdleService() {
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
      private val parallelismGauge: Gauge<Int> = try {
         registry.register<Gauge<Int>>(
            queueName + "-PARALLELISM",
            Gauge<Int> { parallelism }
         )
      } catch (e: Throwable) {
         registry.metrics[queueName + "-PARALLELISM"] as Gauge<Int>
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

      fun next() {
         if (!isRunning) {
            return
         }

         if (activeMessages.get() >= parallelism) {
            return
         }

         val job = jobs.poll() ?: return

         jobsCounter.inc()

         val r = (if (job.request == null) {
            job.actionProvider?.inProvider?.get()
         } else {
            job.request!!
         }) ?: return

         activeMessages.incrementAndGet()
         val actionProvider = job.actionProvider

         actionProvider?.single(r)?.subscribe(
            {
               try {
                  activeMessages.decrementAndGet()

                  if (it != null && it.isSuccess) {
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
                  if (rootCause is HystrixTimeoutException || rootCause is TimeoutException) {
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
      private val DEFAULT_PARALELLISM = Runtime.getRuntime().availableProcessors() * 2
      private val DEFAULT_WORKER_TIMEOUT_MILLIS = 10000
   }
}
