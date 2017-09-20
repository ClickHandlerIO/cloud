package move.action

/**
 *
 */
abstract class DaemonMessage

data class DaemonTimerMessage(val handle: TimerHandle) : DaemonMessage()

abstract class AbstractDaemon : ActorAction() {
   suspend override fun handle(msg: ActorMessage) {
      // Do nothing.
   }
}