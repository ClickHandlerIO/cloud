package move.action

abstract class BaseScheduledAction : Action<Unit, Unit>() {
    suspend override fun recover(caught: Throwable, cause: Throwable, isFallback: Boolean) {
        throw cause
    }
}