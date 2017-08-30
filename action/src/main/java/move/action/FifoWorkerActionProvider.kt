package move.action

import com.google.common.base.Preconditions
import io.vertx.rxjava.core.Vertx
import kotlinx.coroutines.experimental.rx1.await
import rx.Single
import javax.inject.Inject
import javax.inject.Provider

/**

 */
open class FifoWorkerActionProvider<A : Action<IN, Boolean>, IN : Any> @Inject
constructor(vertx: Vertx,
            actionProvider: Provider<A>) : WorkerActionProvider<A, IN>(
   vertx, actionProvider
) {
   /**
    * @param request
    * *
    * @param groupId
    * *
    * @return
    */
   fun send(groupId: String, request: IN): Single<WorkerReceipt> {
      Preconditions.checkNotNull(
         producer,
         "WorkerProducer is null. Ensure ActionManager has been started and all actions have been registered."
      )
      return producer!!.send(WorkerRequest(
         actionProvider = this@FifoWorkerActionProvider as WorkerActionProvider<Action<Any, Boolean>, Any>,
         groupId = groupId,
         request = request
      ))
   }

   /**
    * @param request
    * *
    * @param groupId
    * *
    * @return
    */
   fun send(request: IN, groupId: String): Single<WorkerReceipt> {
      Preconditions.checkNotNull(
         producer,
         "WorkerProducer is null. Ensure ActionManager has been started and all actions have been registered."
      )
      return producer!!.send(WorkerRequest(
         actionProvider = this@FifoWorkerActionProvider as WorkerActionProvider<Action<Any, Boolean>, Any>,
         groupId = groupId,
         request = request
      ))
   }

   /**
    * @param request
    * *
    * @param delaySeconds
    * *
    * @return
    */
   fun send(groupId: String, delaySeconds: Int, request: IN): Single<WorkerReceipt> {
      Preconditions.checkNotNull(
         producer,
         "WorkerProducer is null. Ensure ActionManager has been started and all actions have been registered."
      )

      return producer!!.send(WorkerRequest(
         actionProvider = this@FifoWorkerActionProvider as WorkerActionProvider<Action<Any, Boolean>, Any>,
         delaySeconds = delaySeconds,
         groupId = groupId,
         request = request
      ))
   }
}
