package move.action

import io.vertx.rxjava.core.Vertx
import javaslang.control.Try
import kotlinx.coroutines.experimental.rx1.await
import rx.Observable
import rx.Single
import java.util.function.Consumer
import javax.inject.Inject
import javax.inject.Provider

/**

 */
open class InternalActionProvider<A : Action<IN, OUT>, IN : Any, OUT : Any> @Inject
constructor(vertx: Vertx,
            actionProvider: Provider<A>,
            inProvider: Provider<IN>,
            outProvider: Provider<OUT>) : ActionProvider<A, IN, OUT>(
        vertx, actionProvider, inProvider, outProvider
) {
    override val isInternal = true

    val internalAction: InternalAction? = actionClass.getAnnotation(InternalAction::class.java)

    override fun execute(callable: Try.CheckedConsumer<IN>): OUT {
        return super.execute(callable)
    }

    override fun execute(request: IN): OUT {
        return super.execute(request)
    }

    override fun observe(callback: Consumer<IN>?): Observable<OUT> {
        return super.observe(callback)
    }

    fun single(callback: Consumer<IN>): Observable<OUT> {
        return observe(callback)
    }

    fun observe(data: Any?, request: IN): Observable<OUT> {
        val action = create(request)

        action.context.data = data

        return observe0(action)
    }

    fun single(data: Any?, request: IN): Single<OUT> {
        return observe(data, request).toSingle()
    }

    /**
     * @param request
     * *
     * @return
     */
    override fun observe(request: IN): Observable<OUT> {
        return observe0(create(request))
    }

    suspend operator fun invoke(request: IN): OUT {
        return single(request).await()
    }

    suspend operator inline fun invoke(block: IN.() -> Unit): OUT = invoke(inProvider.get().apply(block))

    suspend fun await(block: IN.() -> Unit): OUT {
        return single(inProvider.get().apply(block)).await()
    }
}
