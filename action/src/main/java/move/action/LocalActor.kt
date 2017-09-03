package move.action

import kotlinx.coroutines.experimental.yield
import java.util.concurrent.TimeUnit


/**
 *
 */
abstract class LocalActor : Action<Unit, Unit>() {
   var running: Boolean = true

   suspend override fun execute() {
      while (running && nextEvent()) {
         yield()
      }
   }

   open suspend fun nextEvent(): Boolean {
      delay(1000, TimeUnit.MILLISECONDS)
      return true
   }

   fun stop() {
      running = false
      coroutine.cancel(LocalActorStopping())
   }
}

class LocalActorStopping : RuntimeException()

/**
 * All subclasses of this class will be automatically started
 * on node startup and stopped on node shutdown.
 */
abstract class AbstractScheduledLocalActor : LocalActor()

/**
 *
 */
abstract class ScheduledLocalActor(
   val interval: Long = 1000L,
   val unit: TimeUnit = TimeUnit.MILLISECONDS) : AbstractScheduledLocalActor() {

   suspend override fun nextEvent(): Boolean {
      sleep(interval, unit)
      return true
   }

   abstract suspend fun next()
}

/**
 * Aligns the interval to the clock.
 */
abstract class AlignedScheduledLocalActor(
   val interval: Long = 1,
   val unit: TimeUnit = TimeUnit.SECONDS) : AbstractScheduledLocalActor() {

   suspend override fun nextEvent(): Boolean {
      sleep(interval, unit)
      return true
   }

   abstract suspend fun next()
}

/**
 * Uses a "CRON" definition to determine when to run.
 */
abstract class CronScheduledLocalActor(
   val interval: Long = 1,
   val unit: TimeUnit = TimeUnit.SECONDS) : AbstractScheduledLocalActor() {

   suspend override fun nextEvent(): Boolean {
      sleep(interval, unit)
      return true
   }

   abstract suspend fun next()
}