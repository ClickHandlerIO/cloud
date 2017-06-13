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

fun isActionTimeout(exception: Throwable): Boolean {
    return when (Throwables.getRootCause(exception)) {
        is HystrixTimeoutException -> true
        is TimeoutException -> true
        else -> false
    }
}
