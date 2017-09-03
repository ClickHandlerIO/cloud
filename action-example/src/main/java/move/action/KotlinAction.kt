package move.action

import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.rx1.await
import move.common.WireFormat
import move.rx.ordered
import move.rx.parallel
import rx.Single
import javax.inject.Inject


@Internal(timeout = 1000)
class MyAction : InternalAction<String, String>() {
   suspend override fun execute(): String {
//      delay(50)
//      val reply = of(AllocateInventory::class)
//         .await(AllocateInventory.Request(id = ""))
//
//      val reply2 = of<AllocateInventory>() await AllocateInventory.Request(id = "")
//
//      val r = of(AllocateInventory::class)..AllocateInventory.Request(id = "")

      return ""
   }
}

/**
 *
 */
@Internal(timeout = 1000)
class Allocate : InternalAction<String, String>() {
   suspend override fun execute(): String {
//      delay(50)
//      val reply = of(AllocateInventory::class)
//         .await(AllocateInventory.Request(id = ""))
//
//      val reply2 = of<AllocateInventory>() await AllocateInventory.Request(id = "")
//
//      val r = of(AllocateInventory::class)..AllocateInventory.Request(id = "")

      return ""
   }
}

@Internal(timeout = 1000)
class Allocate2 : InternalAction<String, String>() {
   suspend override fun execute(): String {
      return ""
   }
}

@Internal(timeout = 1000)
class AllocateStock : InternalAction<AllocateStock.Request, AllocateStock.Reply>() {
   suspend override fun execute(): AllocateStock.Reply {
      of<AllocateStock>()
         .rx(
            AllocateStock.Request(stockId = "STOCK_UID")
         )

      return Reply(code = "FAILED")
   }

   data class Request(val stockId: String = "")

   data class Reply(val code: String = "")
}

@Internal
class AllocateInventory @Inject constructor() : InternalAction<AllocateInventory.Request, AllocateInventory.Reply>() {
   data class Request(var id: String)

   data class Reply(var code: String = "")

   override val isFallbackEnabled = true

   suspend override fun execute(): Reply {
      // Inline blocking coroutineBlock being run asynchronously
      val s = blocking {
         javaClass.simpleName + ": WORKER = " + Thread.currentThread().name
      }
      println(s)

//        val ar = AppComponent.instance.actions().move.action.allocate("Hi")
//        println(ar)

      val blockingParallel = parallel(
         rxBlocking {
            delay(1000)
            println("Worker 1")
            Thread.currentThread().name
         },
         rxBlocking {
            delay(1000)
            println("Worker 2")
            Thread.currentThread().name
         },
         rxBlocking {
            delay(1000)
            println("Worker 3")
            Thread.currentThread().name
         }
      )

      println(blockingParallel)

      val blockingOrdered = ordered(
         rxBlocking {
            delay(1000)
            println("Worker 1")
            Thread.currentThread().name
         },
         rxBlocking {
            delay(1000)
            println("Worker 2")
            Thread.currentThread().name
         },
         rxBlocking {
            delay(1000)
            println("Worker 3")
            Thread.currentThread().name
         }
      )

      run {
         val (v1, v2, v3) = ordered(
            rxBlocking {
               delay(1000)
               println("Worker 1")
               Thread.currentThread().name
            },
            rxBlocking {
               delay(1000)
               println("Worker 2")
               Thread.currentThread().name
            },
            rxBlocking {
               delay(1000)
               println("Worker 3")
               Thread.currentThread().name
            }
         )
      }

      println(blockingOrdered)

      val asyncParallel =
         parallel(
            rx {
               delay(1000)
               println("Async 1")
               Thread.currentThread().name
            },
            rx {
               delay(1000)
               println("Async 2")
               Thread.currentThread().name
            },
            rx {
               delay(1000)
               println("Async 3")
               Thread.currentThread().name
            }
         )

      println(asyncParallel)

      val asyncOrdered = ordered(
         rx {
            delay(1000)
            println("Async 1")
            Thread.currentThread().name
         },
         rx {
            delay(1000)
            println("Async 2")
            Thread.currentThread().name
         },
         rx {
            delay(1000)
            println("Async 3")
            Thread.currentThread().name
         }
      )

      println(asyncOrdered)

      val x = WireFormat.parse(Reply::class.java, "{\"code\":\"TEST\"}")
      println(x)

      return Reply(
         code = "Back At Cha!"
      )
   }

   suspend override fun recover(caught: Throwable, cause: Throwable, isFallback: Boolean): Reply {
      if (isFallback)
         throw cause

      return Reply(code = cause.javaClass.simpleName)
   }
}
