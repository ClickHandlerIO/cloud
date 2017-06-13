package move.action

import com.google.common.base.Preconditions
import io.vertx.rxjava.core.Vertx
import javaslang.control.Try
import rx.Single

import javax.inject.Inject
import java.util.function.Consumer
import javax.inject.Provider

/**

 */
open class FifoWorkerActionProvider<A : Action<IN, Boolean>, IN : Any> @Inject
constructor(vertx: Vertx,
            actionProvider: Provider<A>,
            inProvider: Provider<IN>,
            outProvider: Provider<Boolean>) : WorkerActionProvider<A, IN>(
        vertx, actionProvider, inProvider, outProvider
) {

    /**
     * @param request
     * *
     * @param callback
     */
    override fun send(request: IN, callback: Consumer<Boolean>) {
        send(request, 0, callback)
    }

    /**
     * @param request
     * *
     * @param delaySeconds
     * *
     * @param callback
     */
    override fun send(request: IN, delaySeconds: Int, callback: Consumer<Boolean>) {
        send(request, delaySeconds).subscribe(
                { r -> Try.run { callback.accept(r) } }
        ) { e -> Try.run { callback.accept(false) } }
    }

    /**
     * @param request
     * *
     * @param groupId
     * *
     * @param callback
     */
    fun send(request: IN, groupId: String, callback: Consumer<Boolean>) {
        send(request, groupId).subscribe(
                { r -> Try.run { callback.accept(r) } }
        ) { e -> Try.run { callback.accept(false) } }
    }

    /**
     * @param request
     * *
     * @return
     */
    override fun send(request: IN): Single<Boolean> {
        return send(request, 0)
    }

    /**
     * @param request
     * *
     * @param delaySeconds
     * *
     * @return
     */
    override fun send(request: IN, delaySeconds: Int): Single<Boolean> {
        Preconditions.checkNotNull(
                producer,
                "WorkerProducer is null. Ensure ActionManager has been started and all actions have been registered."
        )
        return producer!!.send(WorkerRequest()
                .actionProvider(this)
                .request(request)
                .delaySeconds(delaySeconds))
    }

    /**
     * @param request
     * *
     * @param groupId
     * *
     * @return
     */
    fun send(request: IN, groupId: String): Single<Boolean> {
        Preconditions.checkNotNull(
                producer,
                "WorkerProducer is null. Ensure ActionManager has been started and all actions have been registered."
        )
        return producer!!.send(WorkerRequest()
                .actionProvider(this)
                .request(request)
                .groupId(groupId))
    }

    /**
     * @param request
     * *
     * @param delaySeconds
     * *
     * @return
     */
    fun send(request: IN, groupId: String, delaySeconds: Int): Single<Boolean> {
        Preconditions.checkNotNull(
                producer,
                "WorkerProducer is null. Ensure ActionManager has been started and all actions have been registered."
        )
        return producer!!.send(WorkerRequest()
                .actionProvider(this)
                .request(request)
                .groupId(groupId)
                .delaySeconds(delaySeconds))
    }
}
