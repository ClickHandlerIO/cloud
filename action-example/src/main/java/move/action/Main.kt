package move.action

import io.reactivex.Single
import io.vertx.core.VertxOptions
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

// Global convenience Variable.
val Move = App.component.locator()

// App
object App : MoveApp<AppComponent>() {

   val LOG = LoggerFactory.getLogger(App::class.java)

   @JvmStatic
   fun main(args: Array<String>) {
      start(args)
   }

   suspend override fun build() = DaggerAppComponent.create()

   suspend override fun vertxOptions(): VertxOptions {
      return super.vertxOptions().setEventLoopPoolSize(Runtime.getRuntime().availableProcessors() * 2)
   }

   suspend override fun startDaemons() {
      val passes = 100
      val parallelism = Runtime.getRuntime().availableProcessors() * 2
      val statsInternval = 1000L
      val invocationsPerPass = 1_000_000
      val actionsPerInvocation = 2

      val dispatcher = eventLoopGroup.executors[0].dispatcher


//      async(dispatcher) {
//         val d = coroutineContext[ContinuationInterceptor.Key]
//         println("Dispatcher ${d}")
//         println("CoroutineContext: $coroutineContext")
//
//         val child1 = async(coroutineContext + CoroutineName("Child-1")) {
//            println("Child-1 CoroutineContext: $coroutineContext")
//            println("Child-1 Dispatcher ${coroutineContext[ContinuationInterceptor.Key]}")
//            println("Child-1 Delaying: ${Thread.currentThread().name}")
//            delay(500)
//            println("Child-1 Dispatcher ${coroutineContext[ContinuationInterceptor.Key]}")
//            println("Child-1 Returned: ${Thread.currentThread().name}")
//            println("Child-1 CoroutineContext: $coroutineContext")
//
//            val child2 = async(coroutineContext + CoroutineName("Child-2")) {
//               println("Child-2 CoroutineContext: $coroutineContext")
//               println("Child-2 Dispatcher ${coroutineContext[ContinuationInterceptor.Key]}")
//               println("Child-2 Delaying: ${Thread.currentThread().name}")
//               delay(500)
//               println("Child-2 Dispatcher ${coroutineContext[ContinuationInterceptor.Key]}")
//               println("Child-2 Returned: ${Thread.currentThread().name}")
//               println("Child-2 CoroutineContext: $coroutineContext")
//            }
//
//            child2.await()
//         }
//
//         child1.await()
//      }.await()

      for (p in 0..10) {
         val future = Move.Allocate rxAsk "Hi"
         future.invokeOnCompletion {
            println(Thread.currentThread().name)
            println("invokeOnCompletion")
         }
         println(Thread.currentThread().name)

         val futureResult = future.await()
         println("Finished with $futureResult")
      }


//      try {

//      val result = Move.Allocate ask "HI"
//
//
//      val list = mutableListOf<Deferred<String>>()
//      launch(dispatcher) {
//         for (i in 1..5) {
//            list += Move.Allocate rxAsk ""
//         }
//      }
//
//      val r = list.map { it.await() }.toList()
//      println("1 Done")
//      } catch (e: Throwable) {
//         println("************** CAUGHT")
//         e.printStackTrace()
//      }


//      val searchStockReply = A.inventory.SearchStock ask ""
//
//      App.vertx.setPeriodic(statsInternval) {
//         if (provider != null) {
//            App.vertx.rxExecuteBlocking<Unit> {
//               val metrics = provider.breaker.metrics.toJson()
////               println("Action:\t${metrics.getString("name")}")
////               println("\t\tRolling Operations: ${metrics.getLong("rollingOperationCount")}")
////               println("\t\tTotal Operations:   ${metrics.getLong("totalOperationCount")}")
////               println("\t\tTotal Success:      ${metrics.getLong("totalSuccessCount")}")
////               println("\t\tTotal CPU:          ${metrics.getLong("totalCpu")}")
////               println("\t\tLatency Mean:       ${metrics.getLong("rollingLatencyMean")}")
////               println("\t\tLatency 50:         ${Duration.of(metrics.getJsonObject("rollingLatency").getLong("50"), ChronoUnit.MICROS)}")
////               println("\t\tLatency 99:         ${metrics.getJsonObject("rollingLatency").getLong("99").toDouble() / 1_000.0}")
////               println("\t\tLatency 99.5:       ${metrics.getJsonObject("rollingLatency").getLong("99.5").toDouble() / 1_000.0}")
////               println("\t\tLatency 100:        ${metrics.getJsonObject("rollingLatency").getLong("100").toDouble() / 1_000.0}")
////               println("\t\tJSON:               ${metrics}")
////               println()
////               println()
//            }.subscribe()
////            provider.breaker.metrics.toJson().getLong("rollingOperationCount")
//         }
//      }

      for (c in 1..passes) {
         var list = mutableListOf<Single<Unit>>()
         val start = System.currentTimeMillis()
         for (t in 0..parallelism-1) {
            val single = Single.create<Unit> { subscriber ->
               val eventLoop = eventLoopGroup.executors[t]
               eventLoop.runOnContext {
                  val counter = AtomicInteger(0)
                  try {
                     for (i in 1..invocationsPerPass) {
//                        AllocateInventory ask ""
                        val call = Move.Allocate.rxAsk("HI")
                        call.invokeOnCompletion {
                           if (counter.incrementAndGet() == invocationsPerPass) {
                              subscriber.onSuccess(Unit)
                           }
                        }
                     }
                  } catch (e: Throwable) {
                     e.printStackTrace()
                  }

//                  launch(eventLoop.dispatcher) {
//                     try {
//                        for (i in 1..invocationsPerPass) {
////                        AllocateInventory ask ""
//                           Move.Allocate ask ""
//                        }
//                     } catch (e: Throwable) {
//                        e.printStackTrace()
//                     }
//                     subscriber.onSuccess(Unit)
//                  }
               }

//               eventLoop.runOnContext {
//                  launch(Unconfined) {
//
//                  }
//               }
            }
            list.add(single)
         }

         Single.zip(list) {}.blockingGet()
         val elapsed = System.currentTimeMillis() - start
         val runsPerSecond = 1000.0 / elapsed
         println("${invocationsPerPass * actionsPerInvocation * runsPerSecond * parallelism} / sec")
      }
   }
}
