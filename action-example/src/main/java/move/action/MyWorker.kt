package move.action

import javax.inject.Inject

/**

 */
@WorkerAction(fifo = true)
class MyWorker @Inject
internal constructor() : BaseWorkerAction<MyWorker.Request>() {
    suspend override fun execute(request: Request): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun recover(caught: Throwable, cause: Throwable, isFallback: Boolean): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    class Request @Inject
    constructor() {
        var id: String? = null
    }
}
