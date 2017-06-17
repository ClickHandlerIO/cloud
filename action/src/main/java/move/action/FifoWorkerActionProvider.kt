package move.action

import com.google.common.base.Preconditions
import io.vertx.rxjava.core.Vertx
import javaslang.control.Try
import kotlinx.coroutines.experimental.rx1.await
import rx.Single

import javax.inject.Inject
import java.util.function.Consumer
import javax.inject.Provider

/**

 */
open class FifoWorkerActionProvider<A : Action<IN, Boolean>, IN : Any> @Inject
constructor(vertx: Vertx,
            actionProvider: Provider<A>,
            inProvider: Provider<IN>) : WorkerActionProvider<A, IN>(
        vertx, actionProvider, inProvider
) {
    /**
     * @param request
     * *
     * @param groupId
     * *
     * @return
     */
    fun single(groupId: String, request: IN): Single<WorkerReceipt> = single(groupId, 0, request)

    /**
     * @param request
     * *
     * @param groupId
     * *
     * @return
     */
    fun single(request: IN, groupId: String): Single<WorkerReceipt> = single(groupId, 0, request)

    /**
     * @param request
     * *
     * @param delaySeconds
     * *
     * @return
     */
    fun single(groupId: String, delaySeconds: Int, request: IN): Single<WorkerReceipt> {
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

    fun single(groupId: String, block: IN.() -> Unit): Single<WorkerReceipt> = single(groupId, 0, block)

    /**
     *
     */
    fun single(groupId: String, delaySeconds: Int, block: IN.() -> Unit): Single<WorkerReceipt> {
        Preconditions.checkNotNull(
                producer,
                "WorkerProducer is null. Ensure ActionManager has been started and all actions have been registered."
        )
        return producer!!.send(WorkerRequest()
                .actionProvider(this)
                .request(inProvider.get().apply(block))
                .groupId(groupId)
                .delaySeconds(delaySeconds))
    }

    /**
     * @param request
     * *
     * @param groupId
     * *
     * @return
     */
    fun send(groupId: String, request: IN): Single<WorkerReceipt> {
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
     * @param groupId
     * *
     * @return
     */
    fun send(request: IN, groupId: String): Single<WorkerReceipt> {
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
    fun send(groupId: String, delaySeconds: Int, request: IN): Single<WorkerReceipt> {
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

    fun send(groupId: String, block: IN.() -> Unit): Single<WorkerReceipt> = send(groupId, 0, block)

    /**
     *
     */
    fun send(groupId: String, delaySeconds: Int, block: IN.() -> Unit): Single<WorkerReceipt> {
        Preconditions.checkNotNull(
                producer,
                "WorkerProducer is null. Ensure ActionManager has been started and all actions have been registered."
        )
        val single = producer!!.send(WorkerRequest()
                .actionProvider(this)
                .request(inProvider.get().apply(block))
                .groupId(groupId)
                .delaySeconds(delaySeconds))

        // Dispatch
        single.subscribe()

        return single
    }

    suspend operator fun invoke(groupId: String, request: IN): Single<WorkerReceipt> = send(groupId, request)

    suspend operator fun invoke(groupId: String, delaySeconds: Int, request: IN): Single<WorkerReceipt> = send(groupId, delaySeconds, request)

    suspend operator fun invoke(groupId: String, block: IN.() -> Unit): Single<WorkerReceipt> = send(groupId, block)

    suspend operator fun invoke(groupId: String, delaySeconds: Int, block: IN.() -> Unit): Single<WorkerReceipt> =
            send(groupId, delaySeconds, block)

    suspend fun await(groupId: String, request: IN): WorkerReceipt = single(groupId, request).await()

    suspend fun await(groupId: String, delaySeconds: Int, request: IN): WorkerReceipt = single(groupId, delaySeconds, request).await()

    suspend fun await(groupId: String, block: IN.() -> Unit): WorkerReceipt =
            single(groupId, block).await()

    suspend fun await(groupId: String, delaySeconds: Int, block: IN.() -> Unit): WorkerReceipt =
            single(groupId, delaySeconds, block).await()
}
