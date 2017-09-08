package move.action

import javax.inject.Inject
//
//
//@Http(
//   method = Http.Method.GET,
//   path = "/catalogue/products/:type/:productid/",
//   consumes = arrayOf("application/json"),
//   produces = arrayOf("application/json")
//)
//class Login : HttpAction() {
//   suspend override fun execute() {
//      val productType = param("type")
//      val id = param("id")
//
//      resp.end("")
//   }
//}
//
@Http(
   method = Http.Method.GET,
   path = "/ping"
)
class Ping : HttpAction() {
   suspend override fun execute() {
      resp.setStatusCode(200).end("PONG")
   }
}
//
//
//@Internal(timeout = 1000)
//class MyAction : InternalAction<String, String>() {
////   companion object : MyAction_Factory()
//
//   suspend override fun execute(): String {
//      // Ask using Factory pattern.
////      AllocateInventory ask { id = "" }
////      AllocateInventory.ask("")
////
////      AllocateInventory rxAsk ""
////      AllocateInventory.rxAsk("")
////
////      // Ask with builder.
////      AllocateInventory ask { id("") }
////
////      MyAction ask ""
//
//      return ""
//   }
//}
//
//
///**
// *
// */
@Internal(timeout = 1000)
class Allocate : InternalAction<String, String>() {
   companion object : Allocate_Producer()

//   companion object : Allocate_Producer()

   suspend override fun execute(): String {

      sleep(5000)

//      MyAction ask ""
//
//      MyAction job ""
//
//      val (r1, r2) = await(
//         MyAction rx "",
//         MyAction rx ""
//      )

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

//
//@Internal(timeout = 1000)
//class Allocate2 : InternalAction<String, String>() {
//   suspend override fun execute(): String {
//      return ""
//   }
//}
//
//@Internal(timeout = 1000)
//class AllocateStock : InternalAction<AllocateStock.Request, AllocateStock.Reply>() {
//   suspend override fun execute(): AllocateStock.Reply {
//      return Reply(code = "FAILED")
//   }
//
//   data class Request(val stockId: String = "")
//
//   data class Reply(val code: String = "")
//}


abstract class SomeJobAction<REQUEST : Any> : InternalAction<REQUEST, AllocateInventory.Reply>() {

}

@Internal
class AllocateInventory @Inject constructor() : SomeJobAction<AllocateInventory.Request>() {
   companion object : AllocateInventory_Producer() {
      suspend infix fun ask(id: String): Reply {
         return ask(Request(id))
      }
   }

   data class Request(var id: String = "")

   fun id(id: String): Request {
      return Request(id = id)
   }

   data class Reply(var code: String = "")

   override val isFallbackEnabled = true

   suspend override fun execute(): Reply {
      // Inline blocking coroutineBlock being run asynchronously
//      val s = blocking {
//         javaClass.simpleName + ": WORKER = " + Thread.currentThread().name
//      }
//      println(s)

//        val ar = AppComponent.instance.actions().move.action.allocate("Hi")
//        println(ar)

//      val blockingParallel = await(
//         rxBlocking {
//            delay(1000)
//            println("Worker 1")
//            Thread.currentThread().name
//         },
//         rxBlocking {
//            delay(1000)
//            println("Worker 2")
//            Thread.currentThread().name
//         },
//         rxBlocking {
//            delay(1000)
//            println("Worker 3")
//            Thread.currentThread().name
//         }
//      )
//
//      println(blockingParallel)
//
//      val blockingOrdered = ordered(
//         rxBlocking {
//            delay(1000)
//            println("Worker 1")
//            Thread.currentThread().name
//         },
//         rxBlocking {
//            delay(1000)
//            println("Worker 2")
//            Thread.currentThread().name
//         },
//         rxBlocking {
//            delay(1000)
//            println("Worker 3")
//            Thread.currentThread().name
//         }
//      )
//
//      run {
//         val (v1, v2, v3) = ordered(
//            rxBlocking {
//               delay(1000)
//               println("Worker 1")
//               Thread.currentThread().name
//            },
//            rxBlocking {
//               delay(1000)
//               println("Worker 2")
//               Thread.currentThread().name
//            },
//            rxBlocking {
//               delay(1000)
//               println("Worker 3")
//               Thread.currentThread().name
//            }
//         )
//      }
//
//      println(blockingOrdered)
//
//      val asyncParallel =
//         await(
//            rx {
//               delay(1000)
//               println("Async 1")
//               Thread.currentThread().name
//            },
//            rx {
//               delay(1000)
//               println("Async 2")
//               Thread.currentThread().name
//            },
//            rx {
//               delay(1000)
//               println("Async 3")
//               Thread.currentThread().name
//            }
//         )
//
//      println(asyncParallel)
//
//      val asyncOrdered = ordered(
//         rx {
//            delay(1000)
//            println("Async 1")
//            Thread.currentThread().name
//         },
//         rx {
//            delay(1000)
//            println("Async 2")
//            Thread.currentThread().name
//         },
//         rx {
//            delay(1000)
//            println("Async 3")
//            Thread.currentThread().name
//         }
//      )
//
//      println(asyncOrdered)
//
//      val x = WireFormat.parse(Reply::class.java, "{\"code\":\"TEST\"}")
//      println(x)

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
