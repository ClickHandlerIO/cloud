package move.action

abstract class KScheduledAction : KAction<Any, Any>() {
    companion object {
        @JvmStatic
        val RESULT = Any()
    }

    suspend override fun execute(request: Any): Any {
        run()
        return RESULT
    }

    suspend abstract fun run()
}