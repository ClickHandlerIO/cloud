package move.action

import io.vertx.rxjava.core.Context
import io.vertx.rxjava.core.Vertx
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlin.coroutines.experimental.CoroutineContext

/**
 *
 */
class VertxContextDispatcher(val actionContext: ActionContext?, val ctx: Context) : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (Vertx.currentContext() != ctx) {
            ctx.runOnContext {
                // Scope Action Context
                Action.contextLocal.set(actionContext)
                try {
                    block.run()
                } finally {
                    Action.contextLocal.remove()
                }
            }
        } else {
            block.run()
        }
    }
}
