package move.action

import io.vertx.core.VertxOptions
import io.vertx.rxjava.core.Vertx
import kotlinx.coroutines.experimental.async
import move.sql.SqlDatabase

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

        async(VertxContextDispatcher(null, vertx.orCreateContext)) {
            Main.actions().kAllocateInventoryBlocking.await(KAllocateInventoryBlocking.Request())

//            try {
//                val result = Single.zip(
//                        Main.actions().kAllocateInventory.single(KAllocateInventory.Request()),
//                        Main.actions().kAllocateInventoryBlocking.single(KAllocateInventoryBlocking.Request()),
//                        Main.actions().kAllocateInventoryBlocking.single(KAllocateInventoryBlocking.Request()),
//                        Tuple::of
//                ).await()
//
//                println(result._1)
//                println(result._2)
//                println(result._3)
//            } catch (e: Throwable) {
//                e.printStackTrace()
//                Throwables.getRootCause(e).printStackTrace()
//            } finally {
//                vertx.close()
//            }
        }
    }
}