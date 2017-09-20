package move.action


abstract class AbstractDaemon : ActorAction() {

   suspend override fun handle(msg: ActorMessage) {
      // Do nothing.
   }
}