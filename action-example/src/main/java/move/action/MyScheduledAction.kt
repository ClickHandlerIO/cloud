package move.action

import kotlinx.coroutines.experimental.rx1.await
import move.common.UID
import javax.inject.Inject

/**
 *
 */
@ActionConfig(maxExecutionMillis = 2000)
@ScheduledAction(intervalSeconds = 1, type = ScheduledActionType.CLUSTER_SINGLETON)
class MyScheduledAction @Inject
constructor() : BaseScheduledAction() {
   suspend override fun recover(caught: Throwable, cause: Throwable, isFallback: Boolean) {
      cause.printStackTrace()
   }

   suspend override fun execute() {
      println(javaClass.simpleName + " " + Thread.currentThread().name)
      val r = MyWorker.Request().apply { id = UID.next() }
      println(r.id)
//      val provider = AppComponent.instance.actions().move.action.myWorker
//      provider(r)
//      AppComponent.instance.actions().move.action.myWorker.single(r)
      val receipt = AppComponent.instance.actions().move.action.myWorker.send {}.await()
      println(receipt.messageId)
   }
}
