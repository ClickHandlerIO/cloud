package move.action

import kotlinx.coroutines.experimental.delay
import javax.inject.Inject

/**
 *
 */
@ActionConfig(maxConcurrentRequests = 1, maxExecutionMillis = 1000)
@InternalAction
class KAllocateInventoryBlocking @Inject
constructor() :
        KBlockingAction<KAllocateInventoryBlocking.Request, KAllocateInventoryBlocking.Response>() {
    override fun isFallbackEnabled() = false

    suspend override fun execute(request: Request): Response {
        println(javaClass.simpleName + ": " + Thread.currentThread().name)
        delay(1500)
        println(javaClass.simpleName + ": " + Thread.currentThread().name)

        return Response().apply { code = "Back At Cha Blocking!" }
    }

    suspend override fun handleException(caught: Throwable, cause: Throwable, isFallback: Boolean): Response {
        return Response().apply{ code = cause.javaClass.simpleName }
    }

    class Request @Inject constructor()

    class Response @Inject constructor() {
        var code: String? = null
    }
}
