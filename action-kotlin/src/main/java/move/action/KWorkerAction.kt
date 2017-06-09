package move.action

abstract class KWorkerAction<T> : KAction<T, Boolean>() {
    companion object {
        @JvmStatic
        val RESULT = Any()
    }
}