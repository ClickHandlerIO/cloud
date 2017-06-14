package move.action

import javax.inject.Inject

/**

 */
@WorkerAction(fifo = true)
class MyWorker @Inject
internal constructor() : BaseWorkerAction<MyWorker.Request>() {

    suspend override fun recover(caught: Throwable, cause: Throwable, isFallback: Boolean): Boolean {
        TODO("not implemented")
    }

    suspend override fun execute(): Boolean {
        TODO("not implemented")
    }

    class Request @Inject
    constructor() {
        var id: String? = null
    }
}
