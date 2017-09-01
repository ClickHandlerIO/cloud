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
}
