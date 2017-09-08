package move.action

import io.reactivex.Single
import kotlinx.coroutines.experimental.async
import org.slf4j.LoggerFactory

// Global convenience Variable.
val A = App.component.locator()

// App
object App : Move<AppComponent>() {
   val LOG = LoggerFactory.getLogger(App::class.java)

   @JvmStatic
   fun main(args: Array<String>) {
      start(args)
   }

   suspend override fun build() = DaggerAppComponent.create()

   suspend override fun startDaemons() {
      val passes = 20
      val parallelism = 1
      val statsInternval = 1000L
      val invocationsPerPass = 1_000_000

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
         for (t in 1..parallelism) {
            val single = Single.create<Unit> { subscriber ->
               val eventLoop = eventLoopGroup.executors[t]
               eventLoop.runOnContext {
                  async(eventLoop.dispatcher) {
                     try {
                        for (i in 1..invocationsPerPass) {
//                        AllocateInventory ask ""
                           Allocate ask ""
                        }
                     } catch (e: Throwable) {
                        e.printStackTrace()
                     }
                     subscriber.onSuccess(Unit)
                  }
               }
            }
            list.add(single)
         }

         Single.zip(list) {}.blockingGet()
         val elapsed = System.currentTimeMillis() - start
         val runsPerSecond = 1000.0 / elapsed
         println("${invocationsPerPass * runsPerSecond * parallelism} / sec")
      }
   }
}
