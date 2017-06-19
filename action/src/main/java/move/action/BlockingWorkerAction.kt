package move.action

import io.vertx.rxjava.core.WorkerExecutor
import kotlinx.coroutines.experimental.rx1.await

abstract class BlockingWorkerAction<T : Any> : Action<T, Boolean>() {
   open fun executor(): WorkerExecutor? {
      return null
   }

   suspend override fun execute(): Boolean {
      val executor = executor()

      if (executor != null) {
         return worker(executor) { executeBlocking() }.await()
      } else {
         return worker { executeBlocking() }.await()
      }
   }

   abstract suspend fun executeBlocking(): Boolean
}