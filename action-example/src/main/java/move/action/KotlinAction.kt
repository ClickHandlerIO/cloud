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


/**
 * @author Clay Molocznik
 */
object KotlinAction {
   @JvmStatic
   fun main(args: Array<String>) {
      // Setup JBoss logging provider for Undertow.
      System.setProperty("org.jboss.logging.provider", "slf4j")
      // Setup IP stack. We want IPv4.
      System.setProperty("java.net.preferIPv6Addresses", "false")
      System.setProperty("java.net.preferIPv4Stack", "true")

      val app = AppComponent.instance
      val actions = app.actions()

      Action.all.forEach { t, u -> println(t) }

      val provider = Action.providerOf<Allocate, String, String>()

      Action.of<Allocate>()

//      val Actions: Action_Locator = generatedComponent.actions().move.action
//      Actions.ensureActionMap()
//      actions.manager.awaitRunning()

      val eventLoopGroup = ActionEventLoopGroup.get(AppComponent.instance.vertx())

//      async(Unconfined) {
//         Actions.allocateInventory {}
//      }

      AppComponent.instance.vertx().setPeriodic(1000L) {
         //         AppComponent.instance.vertx().rxExecuteBlocking<Unit> {
//         actions.root.move.action.allocate
         if (provider != null)
            println(provider.breaker.metrics.toJson().getLong("rollingOperationCount"))
//         }.subscribe()
      }

      async(Unconfined) {
         for (c in 1..20) {
            var list = mutableListOf<Single<Unit>>()
            for (t in 1..6) {
               val single = Single.create<Unit> { subscriber ->
                  //                  async(Unconfined) {
//                     val start = System.currentTimeMillis()
//                     for (i in 1..1000000) {
//                        Actions.allocate.await("")
//                     }
//                     println("${Thread.currentThread().name} ${System.currentTimeMillis() - start} ms")
//                     println(Actions.allocate.breaker?.metrics?.toJson())
//                     subscriber.onSuccess(Unit)
//                  }
                  eventLoopGroup.executors[t].runOnContext {
                     async(Unconfined) {
                        for (i in 1..1_000_000) {
                           Action.of<Allocate>().await("")
//                           Action.of<Allocate>().await("")
                        }
//                        println("${Thread.currentThread().name} ${System.currentTimeMillis() - start} ms")
                        subscriber.onSuccess(Unit)
                     }
                  }
               }

               list.add(single)
            }

            Single.zip(list) {}.await()
         }
      }
//        actions().register()

      eventLoopGroup.executors.forEach {
         //         AppComponent.instance.vertx().rxExecuteBlocking<Unit> {
//         it.runOnContext {
//            async(Unconfined) {
//               val start = System.currentTimeMillis()
//               for (i in 1..1000000) {
//                  Actions.allocate {}
//               }
//               println("${Thread.currentThread().name} ${System.currentTimeMillis() - start} ms")
//               println(Actions.allocate.breaker?.metrics?.toJson())
//            }
//         }

//         }.subscribe()

//         it.runOnContext {
//            async(Unconfined) {
////               for (x in 1..100) {
//
////               }
////         try {
////            val result = Actions.allocateInventory { id = "" }
////
////            println(result.code)
////         } catch (e: Throwable) {
////            e.printStackTrace()
////         }
//            }
//         }
      }
   }
}
//
//
//object Another {
//   @JvmStatic
//   fun main(args: Array<String>) {
//      println("")
//   }
//}
//

/**
 *
 */
//@ActionConfig(maxExecutionMillis = 2000)
//@ScheduledAction(intervalSeconds = 1, type = ScheduledActionType.CLUSTER_SINGLETON)
//class MyScheduledAction @Inject
//constructor() : BaseScheduledAction() {
//   suspend override fun recover(caught: Throwable, cause: Throwable, isFallback: Boolean) {
//      cause.printStackTrace()
//   }
//
//   suspend override fun execute() {
//      println(javaClass.simpleName + " " + Thread.currentThread().name)
////      val r = MyWorker.Request().apply { id = UID.next() }
////      println(r.id)
////      val provider = AppComponent.instance.actions().move.action.myWorker
////      provider(r)
////      AppComponent.instance.actions().move.action.myWorker.single(r)
////      val receipt = AppComponent.instance.actions().move.action.myWorker.send {}.await()
////      println(receipt.messageId)
//   }
//}
//

@InternalAction(timeout = 1000)
class Allocate : Action<String, String>() {
   suspend override fun execute(): String {
      val reply = of(AllocateInventory::class)
         .await(AllocateInventory.Request(id = ""))

      val reply2 = of<AllocateInventory>() await AllocateInventory.Request(id = "")


      val r = of(AllocateInventory::class)..AllocateInventory.Request(id = "")

      return ""
   }
}

@InternalAction(timeout = 1000)
class Allocate2 : Action<String, String>() {
   suspend override fun execute(): String {
      return ""
   }
}

@InternalAction(timeout = 1000)
class AllocateStock : Action<AllocateStock.Request, AllocateStock.Reply>() {
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

@InternalAction
class AllocateInventory @Inject constructor() : Action<AllocateInventory.Request, AllocateInventory.Reply>() {
   override val isFallbackEnabled = true

   suspend override fun recover(caught: Throwable, cause: Throwable, isFallback: Boolean): Reply {
      if (isFallback)
         throw cause

      return Reply(code = cause.javaClass.simpleName)
   }

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

   data class Request(var id: String)

   data class Reply(var code: String = "")
}
//
//
//@WorkerAction(fifo = false)
//class MyWorker @Inject constructor() : BaseWorkerAction<MyWorker.Request>() {
//   suspend override fun recover(caught: Throwable, cause: Throwable, isFallback: Boolean): Boolean {
//      return false
//   }
//
//   suspend override fun execute(): Boolean {
//      println("Started worker")
////      delay(1000)
//      println("Finishing worker")
//      return true
//   }
//
//   class Request @Inject constructor() {
//      var id: String? = null
//   }
//}
//
//
//@ActionConfig(maxExecutionMillis = 10000)
//@ScheduledAction(intervalSeconds = 1, type = ScheduledActionType.NODE_SINGLETON)
//class MyScheduledAction2 @Inject
//constructor() : BaseScheduledAction() {
//   suspend override fun execute() {
////      println(javaClass.simpleName + " " + Thread.currentThread().name)
////      AppComponent.instance.actions().move.action.myWorker.send {}.await()
//   }
//}
