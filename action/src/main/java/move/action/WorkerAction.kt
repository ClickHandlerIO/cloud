package move.action

import io.vertx.rxjava.core.WorkerExecutor


/**
 * WorkerAction where the entire action runs within a thread from a WorkerPool.
 */
abstract class BlockingWorkerAction<IN : Any, OUT : Any> : WorkerAction<IN, OUT>() {
   abstract val executor: WorkerExecutor?

   suspend override fun execute(): OUT {
      if (executor != null) {
         return blocking { executeBlocking() }
//         return blocking(executor!!) { executeBlocking() }
      } else {
         return blocking { executeBlocking() }
      }
   }

   abstract suspend fun executeBlocking(): OUT
}