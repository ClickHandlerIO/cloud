package move.action

abstract class BlockingWorkerAction<T : Any> : Action<T, Boolean>() {
    companion object {
        @JvmStatic
        val RESULT = Any()
    }
}