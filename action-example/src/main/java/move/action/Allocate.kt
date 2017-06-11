package move.action

import javax.inject.Inject

/**

 */
@InternalAction
class Allocate @Inject
constructor() : BaseAction<String, String>() {
    suspend override fun execute(request: String): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun recover(caught: Throwable, cause: Throwable, isFallback: Boolean): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
