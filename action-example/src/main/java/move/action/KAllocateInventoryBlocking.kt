package move.action

import kotlinx.coroutines.experimental.rx1.await
import javaslang.Tuple
import kotlinx.coroutines.experimental.delay
import rx.Single
import javax.inject.Inject

/**
 *
 */
@ActionConfig(maxConcurrentRequests = 1, maxExecutionMillis = 10000)
@InternalAction
class KAllocateInventoryBlocking @Inject
constructor() :
        KBlockingAction<KAllocateInventoryBlocking.Request, KAllocateInventoryBlocking.Reply>() {
    override fun isFallbackEnabled() = false

    suspend override fun execute(request: Request): Reply {
        println(javaClass.simpleName + ": " + Thread.currentThread().name)
//        delay(1500)
        println(javaClass.simpleName + ": " + Thread.currentThread().name)

        val ctx = Action.currentContext()
        println(javaClass.simpleName + ":" + ctx)

//        val r = Main.actions().kAllocateInventory.await(KAllocateInventory.Request())
//        println(r)
//
//        val r2 = Single.zip(
//                Main.actions().kAllocateInventory.single(KAllocateInventory.Request()),
//                Main.actions().kAllocateInventory.single(KAllocateInventory.Request()),
//                Main.actions().kAllocateInventory.single(KAllocateInventory.Request()),
//                Main.actions().kAllocateInventory.single(KAllocateInventory.Request()),
//                Main.actions().kAllocateInventory.single(KAllocateInventory.Request()),
//                Main.actions().kAllocateInventory.single(KAllocateInventory.Request()),
//                Tuple::of
//        ).await()
////
//        println(r2)

//        println(r2._1)
//        println(r2._2)

        return reply {
            code = "Back At Cha Blocking!"
        }
//        throw RuntimeException("Blocking Blowup")
    }

    suspend override fun recover(caught: Throwable, cause: Throwable, isFallback: Boolean): Reply {
        return reply { code = cause.javaClass.simpleName }
    }

    class Request @Inject constructor()

    class Reply @Inject constructor() {
        var code: String? = null

        override fun toString(): String {
            return "Reply(code=$code)"
        }
    }
}
