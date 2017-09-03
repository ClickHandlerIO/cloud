package move.action

/**
 *
 */
class ActorNodeStore {

}

class ProxyActor {

}

/**
 * Actions can be invoked from within an Actor.
 * All Actions will be ordered and it can act like a Fifo
 * queue.
 */
class FifoAction {
   var provider: ActionProvider<*, *, *>? = null
   var request: Any? = null

   var expires: Long = 0
}
