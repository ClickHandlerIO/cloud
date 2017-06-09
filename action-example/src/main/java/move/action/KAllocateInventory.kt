package move.action

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.rx1.await
import move.rx.MoreSingles.zip
import javax.inject.Inject

/**
 *
 */
@ActionConfig(maxExecutionMillis = 1500000)
@InternalAction
class KAllocateInventory @Inject
constructor(val allocate: javax.inject.Provider<Allocate>) :
        KAction<KAllocateInventory.Request, KAllocateInventory.Reply>() {
    override fun isFallbackEnabled() = true

    suspend override fun execute(request: Request): Reply {
        // Inline blocking block being run asynchronously
        val s = blocking {
            println(javaClass.simpleName + ": WORKER = " + Thread.currentThread().name)
        }

        val zipped = zip(
                worker {
                    delay(1000)
                    println("Worker 1")
                    Thread.currentThread().name
                },
                worker {
                    delay(1000)
                    println("Worker 2")
                    Thread.currentThread().name
                },
                worker {
                    delay(1000)
                    println("Worker 3")
                    Thread.currentThread().name
                },
                worker {
                    delay(1000)
                    println("Worker 4")
                    Thread.currentThread().name
                },
                worker {
                    delay(1000)
                    println("Worker 5")
                    Thread.currentThread().name
                }
        ).await()

        println(zipped)

        val or =
                ordered(
                        single {
                            delay(1000)
                            println("Async 1")
                            Thread.currentThread().name
                        },
                        single {
                            delay(1000)
                            println("Async 2")
                            Thread.currentThread().name
                        },
                        single {
                            delay(1000)
                            println("Async 3")
                            Thread.currentThread().name
                        },
                        single {
                            delay(1000)
                            println("Async 4")
                            Thread.currentThread().name
                        },
                        single {
                            delay(1000)
                            println("Async 5")
                            Thread.currentThread().name
                        }
                )

        println(ordered(
                single {
                    delay(1000)
                    println("Async 1")
                    Thread.currentThread().name
                },
                single {
                    delay(1000)
                    println("Async 2")
                    Thread.currentThread().name
                },
                single {
                    delay(1000)
                    println("Async 3")
                    Thread.currentThread().name
                },
                single {
                    delay(1000)
                    println("Async 4")
                    Thread.currentThread().name
                },
                single {
                    delay(1000)
                    println("Async 5")
                    Thread.currentThread().name
                }
        ))

        return reply {
            code = "Back At Cha!"
        }
    }

    suspend override fun recover(caught: Throwable, cause: Throwable, isFallback: Boolean): Reply {
        return reply { code = cause.javaClass.simpleName }
    }

    class Request @Inject constructor()

    class Reply @Inject constructor() {
        var code: String? = null
    }
}
