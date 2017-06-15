package move.action

import io.vertx.rxjava.core.Vertx
import kotlinx.coroutines.experimental.rx1.await
import rx.Observable
import rx.Single
import java.util.function.Consumer
import javax.inject.Inject
import javax.inject.Provider

/**
 * ActionProvider for Remote Actions.
 */
class RemoteActionProvider<A : Action<IN, OUT>, IN : Any, OUT : Any> @Inject
constructor(vertx: Vertx,
            actionProvider: Provider<A>,
            inProvider: Provider<IN>,
            outProvider: Provider<OUT>) : ActionProvider<A, IN, OUT>(
        vertx, actionProvider, inProvider, outProvider
) {
    override val isRemote = true

    /**
     *
     */
    val remoteAction: RemoteAction? = actionClass.getAnnotation(RemoteAction::class.java)

    /**
     *
     */
    val isGuarded: Boolean
        get() = remoteAction?.guarded ?: false

    /**
     * @param callback
     * *
     * @return
     */
    override fun observe(callback: Consumer<IN>?): Observable<OUT> {
        return super.observe(callback)
    }

    fun single(callback: Consumer<IN>): Single<OUT> {
        return observe(callback).toSingle()
    }

    /**
     * @param request
     * *
     * @return
     */
    override fun observe(request: IN): Observable<OUT> {
        return observe0(create(request))
    }

    fun observe(data: Any?, request: IN): Observable<OUT> {
        val action = create(request)

        action.context.data = data

        return observe0(action)
    }

    fun single(data: Any?, request: IN): Single<OUT> {
        return observe(data, request).toSingle()
    }

    fun observe(request: IN, actionCallback: Consumer<A>?): Observable<OUT> {
        val action = create(request)
        actionCallback?.accept(action)
        return observe0(action)
    }

    fun single(request: IN, actionCallback: Consumer<A>): Single<OUT> {
        return observe(request, actionCallback).toSingle()
    }

    suspend operator fun invoke(request: IN): OUT {
        return single(request).await()
    }

    suspend operator inline fun invoke(block: IN.() -> Unit): OUT = invoke(inProvider.get().apply(block))

    suspend fun await(block: IN.() -> Unit): OUT {
        return single(inProvider.get().apply(block)).await()
    }
}
