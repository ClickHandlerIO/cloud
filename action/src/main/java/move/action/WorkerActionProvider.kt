package move.action

import com.google.common.base.Preconditions
import io.vertx.rxjava.core.Vertx
import javaslang.control.Try
import rx.Single
import java.util.function.Consumer
import javax.inject.Inject
import javax.inject.Provider

/**

 */
open class WorkerActionProvider<A : Action<IN, Boolean>, IN : Any> @Inject
constructor(vertx: Vertx,
            actionProvider: Provider<A>,
            inProvider: Provider<IN>,
            outProvider: Provider<Boolean>) : ActionProvider<A, IN, Boolean>(
        vertx, actionProvider, inProvider, outProvider
) {
    val workerAction: WorkerAction? = actionClass.getAnnotation(WorkerAction::class.java)
    val name = actionClass.canonicalName
    val isFifo = workerAction?.fifo ?: false
    val queueName = name.replace(".", "-")

    internal var producer: WorkerProducer? = null

    /**
     * @param request
     * *
     * @param callback
     */
    open fun send(request: IN, callback: Consumer<Boolean>) {
        send(request, 0, callback)
    }

    /**
     * @param request
     * *
     * @param delaySeconds
     * *
     * @param callback
     */
    open fun send(request: IN, delaySeconds: Int, callback: Consumer<Boolean>) {
        send(request, delaySeconds).subscribe(
                { r -> Try.run { callback.accept(r) } }
        ) { e -> Try.run { callback.accept(false) } }
    }

    /**
     * @param request
     * *
     * @return
     */
    open fun send(request: IN): Single<Boolean> {
        return send(request, 0)
    }

    /**
     * @param request
     * *
     * @param delaySeconds
     * *
     * @return
     */
    open fun send(request: IN, delaySeconds: Int): Single<Boolean> {
        Preconditions.checkNotNull(
                producer,
                "WorkerProducer is null. Ensure ActionManager has been started and all actions have been registered."
        )
        return producer!!.send(WorkerRequest()
                .actionProvider(this)
                .request(request)
                .delaySeconds(delaySeconds))
    }
}
