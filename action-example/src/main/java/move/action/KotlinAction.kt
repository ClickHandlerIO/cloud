package move.action

import com.google.common.base.Throwables
import io.vertx.core.VertxOptions
import io.vertx.rxjava.core.Vertx
import javaslang.Tuple
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.rx1.await
import kotlinx.coroutines.experimental.rx1.awaitFirst
import kotlinx.coroutines.experimental.rx1.rxSingle
import move.action.KotlinAction.sql
import move.sql.SqlDatabase
import move.sql.awaitRead
import rx.Observable
import rx.Single

/**
 * @author Clay Molocznik
 */
object KotlinAction {
    val options = VertxOptions()//.setBlockedThreadCheckInterval(6000000)
    val vertx = Vertx.vertx(options)
    val sql: SqlDatabase? = null

    @JvmStatic
    fun main(args: Array<String>) {
//        vertx.runOnContext({ a ->
//            println(Thread.currentThread().name)
//
//            async(VertxContextDispatcher(vertx.orCreateContext)) { BaseAction().run() }
//            println("Dispatched")
//            println(Thread.currentThread().name)
//        })

//        vertx.runOnContext {
//            async(VertxContextDispatcher(Vertx.currentContext())) {
//                try {
//                    Main.actions().kAllocateInventoryBlocking.await(KAllocateInventoryBlocking.Request())
//                    Main.actions().kAllocateInventory.await(KAllocateInventory.Request())
//                } catch (e: Throwable) {
//                    println("Caught You")
//                    Throwables.getRootCause(e).printStackTrace()
//                }
//            }
//        }
//
//        vertx.runOnContext {
//            Main.actions().kAllocateInventory.observe(KAllocateInventory.Request()).subscribe()
//        }
//
//        runBlocking { Main.actions().kAllocateInventory.await(KAllocateInventory.Request()) }

//        async(VertxContextDispatcher(vertx.orCreateContext)) {
//            try {
//                val response = Main.actions().kAllocateInventory.await(KAllocateInventory.Request())
//                val response2 = Main.actions().kAllocateInventoryBlocking.await(KAllocateInventoryBlocking.Request())
//                println(response.code)
//                println(response2.code)
//            } catch (e: Throwable) {
//                Throwables.getRootCause(e).printStackTrace()
//            }
//        }

        async(VertxContextDispatcher(vertx.orCreateContext)) {
            try {
                val result = Single.zip(
//                        Main.actions().kAllocateInventory.single(KAllocateInventory.Request()),
                        Main.actions().kAllocateInventoryBlocking.single(KAllocateInventoryBlocking.Request()),
                        Main.actions().kAllocateInventoryBlocking.single(KAllocateInventoryBlocking.Request()),
                        Tuple::of
                ).await()

                println(result._1)
                println(result._2)
            } catch (e: Throwable) {
                e.printStackTrace()
                Throwables.getRootCause(e).printStackTrace()
            } finally {
                vertx.close()
            }
        }
    }
}


class BaseAction {
    val ctx = KotlinAction.vertx.orCreateContext
    var deferred: Deferred<Unit>? = null
    val contextPool = VertxContextDispatcher(ctx)

    suspend fun run(): String {
        try {
            val id = sql?.awaitRead {
                ""
            }

            val response = Main.actions().kAllocateInventory.await(KAllocateInventory.Request())
            println(response)
            println(response.code)

            val single = rxSingle(contextPool) {
                execute()
            }

            val value = single.await()
            println("Finished: " + value)
            return value
        } catch (e: Throwable) {
            Throwables.getRootCause(e)!!.printStackTrace()
        }
        return ""
    }

    suspend fun execute(): String {
        delay(1000)

        val value = rxSingle(contextPool) {
            println(Thread.currentThread().name)

            delay(1000)
            "Async Waited"
        }.await()

        val value2 = getValue()

        return value2
    }

    suspend fun getValue(): String {
        val single = rxSingle(contextPool) {
            println(Thread.currentThread().name)

            delay(1000)

//            if (true)
//                throw Exception(IllegalArgumentException("Haha wrong!!!"))
//            else
            "Async Waited"
        }

        val value = single.await()

        println(Thread.currentThread().name)

        return value
    }
}
