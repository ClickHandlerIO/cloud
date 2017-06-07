package move.action

import com.google.common.base.Throwables
import com.netflix.hystrix.exception.HystrixTimeoutException
import io.vertx.rxjava.core.Context
import javaslang.Tuple
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.rx1.await
import rx.Observable
import rx.Scheduler
import rx.Single
import rx.functions.Func2
import rx.internal.operators.SingleOperatorZip
import java.util.*
import java.util.concurrent.TimeoutException
import kotlin.coroutines.experimental.CoroutineContext

/**
 *
 */
suspend fun <A : Action<IN, OUT>, IN, OUT> InternalActionProvider<A, IN, OUT>.await(request: IN): OUT {
    return this.single(request).await()
}

suspend fun <A : Action<IN, OUT>, IN, OUT> InternalActionProvider<A, IN, OUT>.awaitAsync(request: IN): OUT {
    return this.singleAsync(request).await()
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

suspend fun <T> Observable<T>.zipAwait() {
    val r = let {
        Single.zip(
                Single.just(""),
                Single.just(1),
                Tuple::of
        ).await()
    }
}

fun isActionTimeout(exception: Throwable): Boolean {
    return when (Throwables.getRootCause(exception)) {
        is HystrixTimeoutException -> true
        is TimeoutException -> true
        else -> false
    }
}

/**
 * Returns a Single that emits the results of a specified combiner function applied to two items emitted by
 * two other Singles.
 *
 *
 * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt=""></img>
 * <dl>
 * <dt>**Scheduler:**</dt>
 * <dd>`zip` does not operate by default on a particular [Scheduler].</dd>
</dl> *

 * @param <T1> the first source Single's value type
 * *
 * @param <T2> the second source Single's value type
 * *
 * @param <R> the result value type
 * *
 * @param s1
 * *            the first source Single
 * *
 * @param s2
 * *            a second source Single
 * *
 * @param zipFunction
 * *            a function that, when applied to the item emitted by each of the source Singles, results in an
 * *            item that will be emitted by the resulting Single
 * *
 * @return a Single that emits the zipped results
 * *
 * @see [ReactiveX operators documentation: Zip](http://reactivex.io/documentation/operators/zip.html)
</R></T2></T1> */
suspend fun <T1, T2, R> awaitZip(s1: Single<out T1>, s2: Single<out T2>, zipFunction: Func2<in T1, in T2, out R>): R {
    return SingleOperatorZip.zip(arrayOf(s1, s2)) { args -> zipFunction.call(args[0] as T1, args[1] as T2) }.await()
}

fun <R> zip(ws: Iterable<Observable<R>>?): Observable<List<R>> {
    if (ws == null || ws.iterator() == null || !ws.iterator().hasNext()) {
        return Observable.just<List<R>>(ArrayList<R>())
    }

    //        ObservableFuture<List<R>> observableFuture = RxHelper.observableFuture();
    //        zipToSingleFile(Lists.newArrayList(ws), observableFuture.toHandler());
    //        return observableFuture;

    return Observable.zip<List<R>>(ws) { args ->
        val responses = ArrayList<R>(args.size)
        for (arg in args) {
            responses.add(arg as R)
        }
        responses
    }
}

class VertxContextDispatcher(val actionContext: ActionContext?, val ctx: Context) : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        ctx.runOnContext {
            // Scope Action Context
            AbstractAction.contextLocal.set(actionContext)
            try {
                block.run()
            } finally {
                AbstractAction.contextLocal.remove()
            }
        }
    }
}
