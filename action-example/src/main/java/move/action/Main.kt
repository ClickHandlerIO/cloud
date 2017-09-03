package move.action

import dagger.Module
import dagger.Provides
import io.vertx.rxjava.core.Vertx
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.rx1.await
import rx.Single
import java.time.Duration
import java.time.temporal.ChronoUnit
import javax.inject.Singleton

object App {
   val vertx: Vertx by lazy { Vertx.vertx() }
   val graph = AppComponent.instance

   init {
      graph.actions()
   }

   @JvmStatic
   fun main(args: Array<String>) {
      val passes = 20
      val parallelism = 6
      val statsInternval = 1000L
      val invocationsPerPass = 1_000_000

//      Action.of<AllocateInventory>().rx(AllocateInventory.Request(id = "")).subscribe()

      val provider = Action.providerOf<Allocate, String, String>()
      val eventLoopGroup = ActionEventLoopGroup.get(App.vertx)

      App.vertx.setPeriodic(statsInternval) {
         if (provider != null) {
            App.vertx.rxExecuteBlocking<Unit> {
               val metrics = provider.breaker.metrics.toJson()
               println("Action:\t${metrics.getString("name")}")
               println("\t\tRolling Operations: ${metrics.getLong("rollingOperationCount")}")
               println("\t\tTotal Operations:   ${metrics.getLong("totalOperationCount")}")
               println("\t\tTotal Success:      ${metrics.getLong("totalSuccessCount")}")
               println("\t\tTotal CPU:          ${metrics.getLong("totalCpu")}")
               println("\t\tLatency Mean:       ${metrics.getLong("rollingLatencyMean")}")
               println("\t\tLatency 50:         ${Duration.of(metrics.getJsonObject("rollingLatency").getLong("50"), ChronoUnit.MICROS)}")
               println("\t\tLatency 99:         ${metrics.getJsonObject("rollingLatency").getLong("99").toDouble() / 1_000.0}")
               println("\t\tLatency 99.5:       ${metrics.getJsonObject("rollingLatency").getLong("99.5").toDouble() / 1_000.0}")
               println("\t\tLatency 100:        ${metrics.getJsonObject("rollingLatency").getLong("100").toDouble() / 1_000.0}")
               println("\t\tJSON:               ${metrics}")
               println()
               println()
            }.subscribe()
//            provider.breaker.metrics.toJson().getLong("rollingOperationCount")
         }
      }

      async(Unconfined) {
         for (c in 1..passes) {
            var list = mutableListOf<Single<Unit>>()
            for (t in 1..parallelism) {
               val single = Single.create<Unit> { subscriber ->
                  eventLoopGroup.executors[t].runOnContext {
                     async(Unconfined) {
                        for (i in 1..invocationsPerPass) {
                           Action.of<Allocate>() await ""
                        }
                        subscriber.onSuccess(Unit)
                     }
                  }
               }
               list.add(single)
            }

            Single.zip(list) {}.await()
         }
      }
   }
}

@Module
class AppModule {
   @Provides
   fun string(): String {
      return ""
   }

   @Provides
   @Singleton
   fun vertx(): Vertx {
      return App.vertx
   }
}
