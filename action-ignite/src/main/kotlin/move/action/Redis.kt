package move.action

import io.vertx.kotlin.circuitbreaker.CircuitBreakerOptions
import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.core.impl.VertxInternal
import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.redis.RedisClient
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.rx1.await
import move.common.UID
import move.rx.await
import java.util.concurrent.atomic.AtomicInteger

/**
 *
 */
object Main {
   @JvmStatic
   fun main(args: Array<String>) {
      val vertx = Vertx.vertx()

      var breaker = CircuitBreaker.create(
         "my-circuit-breaker",
         vertx.delegate,
         CircuitBreakerOptions(
            maxFailures = 5,
            timeout = 500
         )
      )

      val vertxImpl: VertxInternal = vertx.delegate as VertxInternal

      val moveEventLoopGroup = MoveEventLoopGroup(vertxImpl)

      val executors = moveEventLoopGroup.executors

      val iterations = 8000000
      val counter = AtomicInteger()

      val b = breaker

      executors.forEach {
         it.runOnContext {
            println(Thread.currentThread().name)
         }
      }

      val groups = 8
      val batchSize = 1000
      val batches = 1000

      for (i in 0..executors.size - 1) {
         val _c = executors[i]

         val start = System.nanoTime()
         for (bb in 1..batches) {
            _c.runOnContext {
               val c2 = AtomicInteger(0)
               for (a in 1..batchSize) {
                  b.execute<Int>({ future ->
                     future.complete(0)
                  }).setHandler({ ar ->
                     if (c2.incrementAndGet() == batchSize) {
                        if (counter.addAndGet(batchSize) % 1000000 == 0) {
                           val end = System.nanoTime() - start

                           println((1000000 / (end.toDouble() / 1000000.0 / 1000.0)).toString() + " seconds")
                        }
                     }
                  })
               }
            }
         }
      }

      val redis = RedisClient.create(vertx)

//      val circuit = CircuitBreaker().

      async(CommonPool) {
         val get = redis.rxGet("hi").await()

         redis.rxSet("hi2", "Bye 2").await()
         redis.rxSet("hi3", UID.next()).await()

         val (v1, v2, v3) = await(
            redis.rxGet("hi"),
            redis.rxGet("hi2"),
            redis.rxGet("hi3")
         )

         println(v1)
         println(v2)
         println(v3)
      }

      Thread.sleep(400000000)
   }
}