package move.action

import rx.Single
import java.util.concurrent.TimeUnit

/**
 *
 */
abstract class RemoteAction<IN : Any, OUT : Any> : Action<IN, OUT>(), WorkerActionInternal<IN, OUT> {

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
   suspend fun call(request: IN,
                    timeoutUnit: TimeUnit = TimeUnit.SECONDS,
                    timeout: Long = 0,
                    broker: ActionBroker = ActionBroker.DEFAULT): OUT {
      // Send message through PubSub.
      // Set deadline to the timeout.
      // A node will not execute action if it goes past the deadline.

      // Optimize requiring an ACK based on past statistics.
      // If calls come back within 1 second we can save a message.


      // Wait until Response or Timeout expiring.
      return await(request = request)
   }

   fun defer() {

   }

   /**
    * Waits for the Receipt, but does not wait for the result.
    * The receipt allows the result to be tracked.
    */
   suspend fun deferResult(broker: ActionBroker = ActionBroker.DEFAULT): JobReceipt {
      return JobReceipt(id = "", node = "")
   }

   /**
    * Ensure call happens regardless of calling action exiting.
    */
   fun rxDefer() {

   }

   /**
    *
    */
   fun rxCall(request: IN): Single<OUT> {
      job(request, broker = null, ttl = 0)

      return Single.error(RuntimeException())
   }

   /**
    * Calls as a "JOB" using the default broker.
    */
   fun job(request: IN, ttl: Long = 0, broker: ActionBroker? = null) {
      // Always get an ACK
   }

   /**
    * Invokes this action on "this" node.
    */
   override fun rxLocally(request: IN): Single<OUT> {
      return rx(request)
   }
}
