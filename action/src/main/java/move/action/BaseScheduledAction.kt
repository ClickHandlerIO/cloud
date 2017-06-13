package move.action

abstract class BaseScheduledAction : Action<Any, Any>() {
    companion object {
        @JvmStatic
        val RESULT = Any()
    }

    suspend override fun execute(request: Any): Any {
        execute()
        return RESULT
    }

    suspend abstract fun execute()
}