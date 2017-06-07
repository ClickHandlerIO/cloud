package move.action

import javaslang.Tuple
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.rx1.await
import rx.Single
import javax.inject.Inject

/**
 *
 */
@ActionConfig(maxConcurrentRequests = 1, maxExecutionMillis = 10000)
@InternalAction
class KAllocateInventoryBlocking @Inject
constructor() :
        KBlockingAction<KAllocateInventoryBlocking.Request, KAllocateInventoryBlocking.Response>() {
    override fun isFallbackEnabled() = false

    suspend override fun execute(request: Request): Response {
        println(javaClass.simpleName + ": " + Thread.currentThread().name)
//        delay(1500)
        println(javaClass.simpleName + ": " + Thread.currentThread().name)

        val ctx = AbstractAction.contextLocal.get()
        println(javaClass.simpleName + ":" + ctx)

        val r = Main.actions().kAllocateInventory.await(KAllocateInventory.Request())
//        println(r)

        val r2 = Single.zip(
                Main.actions().kAllocateInventory.singleParallel(KAllocateInventory.Request()),
                Main.actions().kAllocateInventory.singleParallel(KAllocateInventory.Request()),
                Main.actions().kAllocateInventory.single(KAllocateInventory.Request()),
//                Main.actions().kAllocateInventory.single(KAllocateInventory.Request()),
                Main.actions().kAllocateInventory.single(KAllocateInventory.Request()),
                Main.actions().kAllocateInventory.singleParallel(KAllocateInventory.Request()),
                Main.actions().kAllocateInventory.singleParallel(KAllocateInventory.Request()),
                Tuple::of
        ).await()

//        println(r2._1)
//        println(r2._2)

//        return Response().apply { code = "Back At Cha Blocking!" }
        throw RuntimeException("Blocking Blewup")
    }

    suspend override fun handleException(caught: Throwable, cause: Throwable, isFallback: Boolean): Response {
        return Response().apply{ code = cause.javaClass.simpleName }
    }

    class Request @Inject constructor()

    class Response @Inject constructor() {
        var code: String? = null

        override fun toString(): String {
            return "Response(code=$code)"
        }
    }
}
