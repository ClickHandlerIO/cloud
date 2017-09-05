package move.action

/**
 *
 */
abstract class ActorAction<IN : Any, OUT : Any> : Action<IN, OUT>() {
   abstract fun forId(groupId: String, id: String)
}