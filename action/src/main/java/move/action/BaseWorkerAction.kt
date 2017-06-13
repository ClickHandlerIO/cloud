package move.action

abstract class BaseWorkerAction<T : Any> : Action<T, Boolean>() {
    companion object {
        @JvmStatic
        val RESULT = Any()
    }
}