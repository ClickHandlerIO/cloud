package move.action

import kotlinx.coroutines.experimental.delay
import javax.inject.Inject

/**

 */
@WorkerAction(fifo = false)
class MyWorker @Inject constructor() : BaseWorkerAction<MyWorker.Request>() {
   suspend override fun recover(caught: Throwable, cause: Throwable, isFallback: Boolean): Boolean {
      return false
   }

   suspend override fun execute(): Boolean {
      println("Started worker")
//      delay(1000)
      println("Finishing worker")
      return true
   }

   class Request @Inject constructor() {
      var id: String? = null
   }
}
