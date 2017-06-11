package move.action

abstract class BaseWorkerAction<T> : BaseAction<T, Boolean>() {
    companion object {
        @JvmStatic
        val RESULT = Any()
    }
}