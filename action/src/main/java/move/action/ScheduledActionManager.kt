package move.action

import com.google.common.base.Preconditions
import com.google.common.util.concurrent.AbstractExecutionThreadService
import com.google.common.util.concurrent.AbstractIdleService
import com.google.common.util.concurrent.AbstractScheduledService
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.core.ILock
import io.vertx.rxjava.core.Vertx
import javaslang.control.Try
import move.cluster.HazelcastProvider
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**

 */
@Singleton
class ScheduledActionManager @Inject
internal constructor(val vertx: Vertx,
                     hazelcastProvider: HazelcastProvider?) : AbstractIdleService() {
    private val clusterSingletons = ArrayList<ClusterSingleton>()
    private val nodeSingletons = ArrayList<NodeSingleton>()

    internal var hazelcastInstance: HazelcastInstance? = hazelcastProvider?.get()
    @Throws(Exception::class)
    override fun startUp() {
        ActionManager.scheduledActionMap.values.forEach { scheduledActionProvider ->
            when (scheduledActionProvider.scheduledAction.type) {
                ScheduledActionType.CLUSTER_SINGLETON -> clusterSingletons.add(ClusterSingleton(scheduledActionProvider))
                ScheduledActionType.NODE_SINGLETON -> nodeSingletons.add(NodeSingleton(scheduledActionProvider))
            }
        }

        nodeSingletons.forEach { value -> value.startAsync().awaitRunning() }
        clusterSingletons.forEach { value -> value.startAsync().awaitRunning() }
    }

    @Throws(Exception::class)
    override fun shutDown() {
        clusterSingletons.forEach { value -> value.stopAsync().awaitTerminated() }
        nodeSingletons.forEach { value -> value.stopAsync().awaitTerminated() }
    }

    /**

     */
    private inner class ClusterSingleton(private val provider: ScheduledActionProvider<*>) : AbstractExecutionThreadService() {
        private val intervalSeconds: Int
        private var thread: Thread? = null
        private var startup = true

        init {
            this.intervalSeconds = provider.scheduledAction.intervalSeconds

            Preconditions.checkNotNull(intervalSeconds, "ScheduledAction: " +
                    provider.actionClass.canonicalName +
                    " has an invalid value for intervalSeconds() = " +
                    intervalSeconds)
        }

        override fun serviceName(): String {
            return provider.actionClass.canonicalName
        }

        @Throws(Exception::class)
        override fun startUp() {
        }

        override fun triggerShutdown() {
            Try.run { thread!!.interrupt() }
        }

        @Throws(Exception::class)
        override fun run() {
            thread = Thread.currentThread()
            while (isRunning && !Thread.interrupted()) {
                try {
                    if (hazelcastInstance == null) {
                        try {
                            while (isRunning) {
                                doRun()
                            }
                        } catch (e: InterruptedException) {
                            return
                        } catch (e: Exception) {
                            LOG.error("Error Running Standalone Scheduled Action  " + provider.actionClass.canonicalName, e)
                            return
                        }

                    } else {
                        var lock: ILock? = null

                        try {
                            lock = hazelcastInstance!!.getLock(provider.actionClass.canonicalName)
                            lock.lockInterruptibly()
                        } catch (e: InterruptedException) {
                            return
                        } catch (e1: Exception) {
                            LOG.info("Failed to get cluster lock for Scheduled Action " + provider.actionClass.canonicalName, e1)
                        }

                        try {
                            while (isRunning && !Thread.interrupted()) {
                                try {
                                    doRun()
                                } catch (e: InterruptedException) {
                                    return
                                } catch (e1: Exception) {
                                    LOG.error("Error Running Hazelcast Scheduled Action " + provider.actionClass.canonicalName, e1)
                                }

                            }
                        } finally {
                            assert(lock != null)
                            lock!!.unlock()
                        }
                    }
                } catch (e: Throwable) {
                    // Ignore.
                    LOG.warn("Failed to run Scheduled Action " + provider.actionClass.canonicalName, e)
                }

            }
        }

        @Throws(InterruptedException::class)
        private fun doRun() {
            val start = System.currentTimeMillis()

            if (startup) {
                startup = false
            } else {
                provider.blockingBuilder(Unit)
            }

            val elapsed = System.currentTimeMillis() - start
            val sleepFor = TimeUnit.SECONDS.toMillis(intervalSeconds.toLong()) - elapsed

            try {
                if (sleepFor > 0) {
                    Thread.sleep(sleepFor)
                }
            } catch (e: InterruptedException) {
                // Do nothing.
                throw e
            }

        }
    }

    /**

     */
    private inner class NodeSingleton(private val provider: ScheduledActionProvider<*>) : AbstractScheduledService() {
        private var startup = true

        override fun serviceName(): String {
            return provider.actionClass.canonicalName
        }

        @Throws(Exception::class)
        override fun runOneIteration() {
            try {
                run()
            } catch (e: InterruptedException) {
                LOG.warn(provider.actionClass.canonicalName, e)
                throw e
            } catch (e: Throwable) {
                LOG.warn(provider.actionClass.canonicalName, e)
            }
        }

        @Throws(InterruptedException::class)
        protected fun run() {
            if (startup) {
                startup = false
            } else {
                provider.blockingBuilder(Unit)
            }
        }

        override fun scheduler(): AbstractScheduledService.Scheduler {
            return AbstractScheduledService.Scheduler.newFixedRateSchedule(
                    0,
                    provider.scheduledAction.intervalSeconds.toLong(),
                    TimeUnit.SECONDS
            )
        }
    }

    companion object {
        private val EMPTY = Any()
        private val LOG = LoggerFactory.getLogger(ScheduledActionManager::class.java)
    }
}
