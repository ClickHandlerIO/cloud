package move.action

import javax.inject.Inject

/**

 */
@ActionConfig(maxExecutionMillis = 10000)
@ScheduledAction(intervalSeconds = 1, type = ScheduledActionType.NODE_SINGLETON)
class MyScheduledAction2 @Inject
constructor() : BaseScheduledAction() {
   suspend override fun execute() {
      println(javaClass.simpleName + " " + Thread.currentThread().name)
   }
}
