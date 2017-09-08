package move.action

import io.vertx.rxjava.core.Vertx
import kotlinx.coroutines.experimental.yield
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider


/**
 *
 */
abstract class DaemonAction : InternalAction<Unit, Unit>() {
   suspend override fun execute() {
      while (isActive && nextEvent()) {
         yield()
      }
   }

   open suspend fun nextEvent(): Boolean {
      delay(1000, TimeUnit.MILLISECONDS)
      return true
   }

   fun stop() {
      cancel(KillPill())
   }
}

class KillPill : RuntimeException()


/**
 * Uses a "CRON" definition to determine when to run.
 */
abstract class CronDaemon(
   val interval: Long = 1,
   val unit: TimeUnit = TimeUnit.SECONDS) : DaemonAction() {

   suspend override fun nextEvent(): Boolean {
      sleep(interval, unit)
      return true
   }

   abstract suspend fun next()
}

open class DaemonActionProvider<A : DaemonAction>
@Inject constructor(vertx: Vertx, provider: Provider<A>)
   : InternalActionProvider<A, Unit, Unit>(vertx, provider) {

}