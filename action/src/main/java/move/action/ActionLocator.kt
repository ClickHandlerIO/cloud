package move.action

import java.util.*
import javax.inject.Inject

/**

 */
abstract class ActionLocator {
   protected val children = mutableSetOf<ActionLocator>()
   val actionMap: MutableMap<Any, ActionProvider<Action<Any, Any>, Any, Any>> = HashMap()
   var actionManager: ActionManager? = null
   private var inited: Boolean = false

   @Inject
   internal fun setActionManager(actionManager: ActionManager) {
      this.actionManager = actionManager
   }

   fun register(actionMap: MutableMap<Any, ActionProvider<Action<Any, Any>, Any, Any>>) {
      ensureActionMap()

      children.forEach { locator ->
         val childActions = locator.ensureActionMap()
         if (childActions != null) {
            actionMap.putAll(childActions)
         }
      }

      if (actionMap != this.actionMap) {
         this.actionMap += actionMap
      }

//      actionMap.forEach { key, value ->
//         if (value.javaClass.isAssignableFrom(RemoteActionProvider::class.java)) {
//            if (key is String) {
//               if (remoteActionMap.containsKey(key)) {
//                  val actionProvider = remoteActionMap[key]
//                  throw RuntimeException("Duplicate Remote Entry for key [" + key + "]. " +
//                     value.actionClass.canonicalName + " and " +
//                     actionProvider!!.actionClass.canonicalName)
//               }
//            }
//            remoteActionMap.put(key, value as RemoteActionProvider<Action<Any, Any>, Any, Any>)
//         } else if (value.javaClass.isAssignableFrom(InternalActionProvider::class.java)) {
//            internalActionMap.put(key, value as InternalActionProvider<Action<Any, Any>, Any, Any>)
//         } else if (value.javaClass.isAssignableFrom(WorkerActionProvider::class.java)) {
//            workerActionMap.put(key, value as WorkerActionProvider<Action<Any, Boolean>, Any>)
//            workerActionMap.put(value.actionClass.canonicalName, value)
//
//            var list: Set<WorkerActionProvider<Action<Any, Boolean>, Any>>? = workerActionQueueGroupMap[value.queueName]
//            if (list == null) {
//               list = setOf()
//            }
//            list += value
//
//            workerActionQueueGroupMap.put(value.queueName, list)
//         } else if (value.javaClass.isAssignableFrom(ScheduledActionProvider::class.java)) {
//            scheduledActionMap.put(key, value as ScheduledActionProvider<Action<Unit, Unit>>)
//         } else {
//            throw RuntimeException("ActionType: " + value.actionClass.canonicalName + ": is an unknown type.");
//         }
//      }
   }

   fun ensureActionMap(): Map<Any, ActionProvider<Action<Any, Any>, Any, Any>>? {
      init()
      return actionMap
   }

   fun put(cls: Any, provider: ActionProvider<*, *, *>) {
      actionMap.put(cls, provider as ActionProvider<Action<Any, Any>, Any, Any>)
   }

   fun put(map: Map<Class<*>, ActionProvider<*, *, *>>) {
      map.forEach { put(it.key, it.value) }
   }

   operator fun <A : Action<IN, OUT>, IN : Any, OUT : Any> get(cls: Any): ActionProvider<A, IN, OUT> {
      return actionMap[cls] as ActionProvider<A, IN, OUT>
   }

   @Synchronized private fun init() {
      if (inited)
         return

      inited = true

      // Init actions.
      initActions()

      // Load child locators.
      initChildren()

      register(actionMap)
   }

   protected open fun initActions() {

   }

   protected open fun initChildren() {

   }
}
