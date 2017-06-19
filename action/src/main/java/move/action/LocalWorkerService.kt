package move.action

import com.google.common.util.concurrent.AbstractExecutionThreadService
import com.google.common.util.concurrent.AbstractIdleService
import io.vertx.rxjava.core.Vertx
import javaslang.control.Try
import move.common.UID
import org.slf4j.LoggerFactory
import rx.Single
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**

 */
@Singleton
class LocalWorkerService @Inject
internal constructor(val vertx: Vertx) : AbstractIdleService(), WorkerService, WorkerProducer {

   private val queue = LinkedBlockingDeque<WorkerRequest>()
   private val consumer = Consumer()

   @Throws(Exception::class)
   override fun startUp() {
      ActionManager.workerActionMap.values.forEach { provider -> provider.producer = this }
      consumer.startAsync().awaitRunning()
   }

   @Throws(Exception::class)
   override fun shutDown() {
      consumer.stopAsync().awaitTerminated()
   }

   override fun send(request: WorkerRequest): Single<WorkerReceipt> {
      return Single.create<WorkerReceipt> { subscriber ->

         if (request.delaySeconds > 0) {
            vertx.setTimer(
               TimeUnit.SECONDS.toMillis(request.delaySeconds.toLong())
            ) { event -> queue.add(request) }
         } else {
            queue.add(request)
         }

         subscriber.onSuccess(WorkerReceipt().apply {
            messageId = UID.next()
         })
      }
   }

   private inner class Consumer : AbstractExecutionThreadService() {
      private var thread: Thread? = null

      override fun triggerShutdown() {
         Try.run { thread!!.interrupt() }
      }

      @Throws(Exception::class)
      override fun run() {
         thread = Thread.currentThread()

         while (isRunning) {
            try {
               doRun()
            } catch (e: InterruptedException) {
               return
            } catch (e: Throwable) {
               // Ignore.
               LOG.error("Unexpected exception", e)
            }

         }
      }

      @Throws(InterruptedException::class)
      protected fun doRun() {
         val request = queue.take() ?: return

         request.actionProvider.single0(request.request).subscribe()
      }
   }

   companion object {
      internal val LOG = LoggerFactory.getLogger(LocalWorkerService::class.java)
   }
}
