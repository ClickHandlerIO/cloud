package move.action

import com.google.common.base.Throwables
import com.netflix.hystrix.exception.HystrixTimeoutException
import io.vertx.rxjava.core.Context
import io.vertx.rxjava.core.Vertx
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.rx1.await
import move.Tuple
import move.Tuple5
import move.rx.MoreSingles
import move.rx.SingleOperatorOrderedZip
import rx.Observable
import rx.Single
import rx.functions.FuncN
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

suspend fun <T1, T2, T3, T4, T5> ordered(s1: Single<out T1>, s2: Single<out T2>, s3: Single<out T3>, s4: Single<out T4>, s5: Single<out T5>): Tuple5<T1, T2, T3, T4, T5> {
    return SingleOperatorOrderedZip.zip(arrayOf(s1, s2, s3, s4, s5)) { args -> Tuple.of(args[0] as T1, args[1] as T2, args[2] as T3, args[3] as T4, args[4] as T5) }.await()
}
