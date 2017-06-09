package move.action

import com.google.common.base.Throwables
import com.netflix.hystrix.exception.HystrixTimeoutException
import io.vertx.rxjava.core.Context
import io.vertx.rxjava.core.Vertx
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.rx1.await
import move.*
import move.rx.MoreSingles
import move.rx.SingleOperatorOrderedZip
import rx.Observable
import rx.Scheduler
import rx.Single
import rx.functions.*
import java.util.*
import java.util.concurrent.TimeoutException
import kotlin.coroutines.experimental.CoroutineContext

/**
 *
 */
suspend fun <A : Action<IN, OUT>, IN, OUT> InternalActionProvider<A, IN, OUT>.await(request: IN): OUT {
    return this.single(request).await()
}

suspend fun <A : Action<IN, OUT>, IN, OUT> InternalActionProvider<A, IN, OUT>.await(data: Any?, request: IN): OUT {
    return this.single(data, request).await()
}

suspend fun <A : Action<IN, OUT>, IN, OUT> RemoteActionProvider<A, IN, OUT>.await(request: IN): OUT {
    return this.single(request).await()
}

suspend fun <A : Action<IN, OUT>, IN, OUT> RemoteActionProvider<A, IN, OUT>.await(data: Any?, request: IN): OUT {
    return this.single(data, request).await()
}

fun isActionTimeout(exception: Throwable): Boolean {
    return when (Throwables.getRootCause(exception)) {
        is HystrixTimeoutException -> true
        is TimeoutException -> true
        else -> false
    }
}

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
