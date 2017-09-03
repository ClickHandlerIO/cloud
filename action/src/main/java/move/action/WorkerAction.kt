package move.action

import kotlinx.coroutines.experimental.rx1.await
import rx.Single
import java.util.concurrent.TimeUnit

/**
 *
 */
abstract class WorkerAction<IN : Any, OUT : Any> : Action<IN, OUT>(), WorkerActionInternal<IN, OUT> {

   var id: String = ""
   var cycle: String = ""
   var index = 0

   val globalID
      get() = ActionManager.NODE_ID + id

   /**
    * Invokes with reliability of a function call.
    *
    * Gives a deadline based on the timeout specified.
    */
   suspend fun call(request: IN, timeoutUnit: TimeUnit, timeout: Long): OUT {
      // Send message through PubSub.
      // Set deadline to the timeout.
      // A node will not execute action if it goes past the deadline.

      // Optimize requiring an ACK based on past statistics.
      // If calls come back within 1 second we can save a message.


      // Wait until Response or Timeout expiring.
      return await(request = request)
   }

   /**
    *
    */
   fun rxCall(request: IN): Single<OUT> {
      return Single.error(RuntimeException())
   }

   /**
    * Uses the "JOB" pattern.
    *
    * Confirms receipt of acceptance first
    */
   fun job(request: IN) {
      // Always get an ACK
   }

   /**
    * Invokes this action on "this" node.
    */
   override fun rxLocally(request: IN): Single<OUT> {
      return rx(request)
   }
}

/**
 * Internal API. Use a responsibly.
 */
interface WorkerActionInternal<IN : Any, OUT : Any> {
   fun rxLocally(request: IN): Single<OUT>
}

/**
 * Keeps track of a Job
 */
class Job<T>(val receipt: Single<JobReceipt>, val result: Single<T>) {
   suspend fun awaitACK(): JobReceipt {
      return receipt.await()
   }

   suspend fun await(): T {
      return result.await()
   }
}

class Call<T> {
   var rx: Single<T> = Single.just(null)
}

val DEFAULT_BROKER
   get() = false

data class JobReceipt(val id: String, val node: String)

