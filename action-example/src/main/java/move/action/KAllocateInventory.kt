package move.action

import kotlinx.coroutines.experimental.delay
import javax.inject.Inject

/**
 *
 */
@ActionConfig(maxExecutionMillis = 1000)
@InternalAction
class KAllocateInventory @Inject
constructor() :
        KAction<KAllocateInventory.Request, KAllocateInventory.Response>() {
    override fun isFallbackEnabled() = true

    suspend override fun execute(request: Request): Response {
        delay(1500)
        throw RuntimeException("Hahahaha")
//        println(javaClass.simpleName + ": " + Thread.currentThread().name)
//        delay(4500)
////        Thread.sleep(4500L)
//        println(javaClass.simpleName + ": " + Thread.currentThread().name)
//
//        return Response().apply { code = "Back At Cha!" }
    }

    suspend override fun handleException(caught: Throwable, cause: Throwable, isFallback: Boolean): Response {
//        throw cause
        return Response().apply { code = cause.javaClass.simpleName }
    }

    class Request @Inject constructor()

    class Response @Inject constructor() {
        var code: String? = null

        override fun toString(): String {
            return "Response(code=$code)"
        }
    }
}
