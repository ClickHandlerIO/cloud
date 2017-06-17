package move.action

import io.vertx.rxjava.core.Vertx
import javaslang.control.Try
import kotlinx.coroutines.experimental.rx1.await
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

    fun blockingLocal(request: IN): OUT = blocking(request)

    fun execute(callable: Try.CheckedConsumer<IN>): OUT = super.execute0(callable)

    fun execute(request: IN): OUT = super.execute0(request)

    fun blocking(request: IN): OUT = super.blockingBuilder(request)

    fun blocking(data: Any?, request: IN): OUT = super.blockingBuilder(data, request)

    fun blocking(block: IN.() -> Unit): OUT = super.blockingBuilder(inProvider.get().apply(block))

    fun blocking(data: Any?, block: IN.() -> Unit): OUT = super.blockingBuilder(data, inProvider.get().apply(block))

    fun singleBuilder(callback: Consumer<IN>): Single<OUT> {
        val request = inProvider.get()
        callback.accept(request)
        return super.single0(request)
    }

    fun singleBuilder(request: IN): Single<OUT> {
        return super.single0(request)
    }

    fun singleBuilder(data: Any?, request: IN): Single<OUT> {
        return super.single0(data, request)
    }

    fun single(request: IN): Single<OUT> {
        return super.single0(request)
    }

    fun single(data: Any?, request: IN): Single<OUT> {
        return super.single0(data, request)
    }

    fun single(block: IN.() -> Unit): Single<OUT> {
        return super.single0(inProvider.get().apply(block))
    }

    fun single(data: Any?, block: IN.() -> Unit): Single<OUT> {
        return super.single0(data, inProvider.get().apply(block))
    }

    fun eagerSingle(callback: Consumer<IN>): Single<OUT> {
        val request = inProvider.get()
        callback.accept(request)
        return super.eagerSingle0(request)
    }

    fun eagerSingle(data: Any?, request: IN): Single<OUT> = super.eagerSingle0(data, request)

    fun eagerSingle(block: IN.() -> Unit): Single<OUT> = super.eagerSingle0(inProvider.get().apply(block))

    fun eagerSingle(data: Any?, block: IN.() -> Unit): Single<OUT> =
            super.eagerSingle0(data, inProvider.get().apply(block))

    suspend operator fun invoke(request: IN): OUT = await(request)

    suspend operator fun invoke(data: Any?, request: IN): OUT = await(data, request)

    suspend operator fun invoke(block: IN.() -> Unit): OUT = await(inProvider.get().apply(block))

    suspend operator fun invoke(data: Any?, block: IN.() -> Unit): OUT = await(data, block)

    suspend fun await(request: IN): OUT = super.single0(request).await()

    suspend fun await(data: Any?, request: IN): OUT = super.single0(data, request).await()

    suspend fun await(block: IN.() -> Unit): OUT = super.single0(inProvider.get().apply(block)).await()

    suspend fun await(data: Any?, block: IN.() -> Unit): OUT = super.single0(data, inProvider.get().apply(block)).await()
}
