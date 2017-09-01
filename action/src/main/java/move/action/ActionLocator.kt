package move.action

import java.util.*
import javax.inject.Inject

/**

 */
abstract class ActionLocator {
   protected val children = mutableSetOf<ActionLocator>()
   val remoteActionMap = HashMap<Any, RemoteActionProvider<Action<Any, Any>, Any, Any>>()
   val internalActionMap = HashMap<Any, InternalActionProvider<Action<Any, Any>, Any, Any>>()
   val workerActionMap = HashMap<Any, WorkerActionProvider<Action<Any, Boolean>, Any>>()
   val workerActionQueueGroupMap: HashMap<String, Set<WorkerActionProvider<Action<Any, Boolean>, Any>>> =
      LinkedHashMap<String, Set<WorkerActionProvider<Action<Any, Boolean>, Any>>>()
   val scheduledActionMap = HashMap<Any, ScheduledActionProvider<Action<Unit, Unit>>>()
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

      actionMap.forEach { key, value ->
         if (value.javaClass.isAssignableFrom(RemoteActionProvider::class.java)) {
            if (key is String) {
               if (remoteActionMap.containsKey(key)) {
                  val actionProvider = remoteActionMap[key]
                  throw RuntimeException("Duplicate Remote Entry for key [" + key + "]. " +
                     value.actionClass.canonicalName + " and " +
                     actionProvider!!.actionClass.canonicalName)
               }
            }
            remoteActionMap.put(key, value as RemoteActionProvider<Action<Any, Any>, Any, Any>)
         } else if (value.javaClass.isAssignableFrom(InternalActionProvider::class.java)) {
            internalActionMap.put(key, value as InternalActionProvider<Action<Any, Any>, Any, Any>)
         } else if (value.javaClass.isAssignableFrom(WorkerActionProvider::class.java)) {
            workerActionMap.put(key, value as WorkerActionProvider<Action<Any, Boolean>, Any>)
            workerActionMap.put(value.actionClass.canonicalName, value)

            var list: Set<WorkerActionProvider<Action<Any, Boolean>, Any>>? = workerActionQueueGroupMap[value.queueName]
            if (list == null) {
               list = setOf()
            }
            list += value

            workerActionQueueGroupMap.put(value.queueName, list)
         } else if (value.javaClass.isAssignableFrom(FifoWorkerActionProvider::class.java)) {
            workerActionMap.put(key, value as FifoWorkerActionProvider<Action<Any, Boolean>, Any>)
            workerActionMap.put(value.actionClass.canonicalName, value)

            var list: Set<WorkerActionProvider<Action<Any, Boolean>, Any>>? = workerActionQueueGroupMap[value.queueName]
            if (list == null) {
               list = setOf()
            }
            list += value

            workerActionQueueGroupMap.put(value.queueName, list)
         } else if (value.javaClass.isAssignableFrom(ScheduledActionProvider::class.java)) {
            scheduledActionMap.put(key, value as ScheduledActionProvider<Action<Unit, Unit>>)
         } else {
            throw RuntimeException("ActionType: " + value.actionClass.canonicalName + ": is an unknown type.");
         }
      }
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

   fun bindProducer(provider: WorkerActionProvider<*, *>, producer: WorkerProducer) {
      provider.producer = producer
   }

   fun timeoutEnabled(enabled: Boolean) {
      actionMap.forEach { k, v -> v.isExecutionTimeoutEnabled = enabled }
   }

   operator fun <A : Action<IN, OUT>, IN : Any, OUT : Any> get(cls: Any): ActionProvider<A, IN, OUT> {
      return actionMap[cls] as ActionProvider<A, IN, OUT>
   }

   /**
    * @param cls
    * *
    * @param <A>
    * *
    * @param <IN>
    * *
    * @param <OUT>
    * *
    * @return
   </OUT></IN></A> */
   fun <A : Action<IN, OUT>, IN : Any, OUT : Any> locate(cls: String): ActionProvider<A, IN, OUT> {
      return ensureActionMap()!![cls] as ActionProvider<A, IN, OUT>
   }

   /**
    * @param key
    * *
    * @param <A>
    * *
    * @param <IN>
    * *
    * @param <OUT>
    * *
    * @return
   </OUT></IN></A> */
   fun <A : Action<IN, OUT>, IN : Any, OUT : Any> locate(key: Any): ActionProvider<A, IN, OUT> {
      return ensureActionMap()!![key] as ActionProvider<A, IN, OUT>
   }

   /**
    * @param key
    * *
    * @param <A>
    * *
    * @param <IN>
    * *
    * @param <OUT>
    * *
    * @return
   </OUT></IN></A> */
   fun <A : Action<IN, OUT>, IN : Any, OUT : Any> locateRemote(key: Any): ActionProvider<A, IN, OUT> {
      ensureActionMap()
      return remoteActionMap[key] as RemoteActionProvider<A, IN, OUT>
   }

   /**
    * @return
    */
   val providerMap: Map<Any, ActionProvider<*, *, *>>
      get() = HashMap(ensureActionMap()!!)

   /**
    * @return
    */
   val internalProviderMap: Map<Any, InternalActionProvider<*, *, *>>
      get() {
         ensureActionMap()
         return HashMap(internalActionMap)
      }

   /**
    * @return
    */
   val remoteProviderMap: Map<Any, RemoteActionProvider<*, *, *>>
      get() {
         ensureActionMap()
         return HashMap(remoteActionMap)
      }

   val workerProviderMap: Map<Any, WorkerActionProvider<*, *>>
      get() {
         ensureActionMap()
         return HashMap(workerProviderMap)
      }

   val scheduledProviderMap: Map<Any, ScheduledActionProvider<*>>
      get() {
         ensureActionMap()
         return HashMap(scheduledActionMap)
      }

   /**
    * @return
    */
   val remotePathMap: Map<String, RemoteActionProvider<*, *, *>>
      get() {
         ensureActionMap()
         val map = HashMap<String, RemoteActionProvider<*, *, *>>()
         remoteActionMap.forEach { key, value -> map.put(value.remote!!.path, value) }
         return map
      }

   @Synchronized private fun init() {
      if (inited)
         return

      inited = true

      if (this == ActionManager) {
         return
      }

      // Init actions.
      initActions()

      // Load child locators.
      initChildren()

      register(actionMap)
      ActionManager.register(actionMap)
   }

   protected open fun initActions() {

   }

   protected open fun initChildren() {

   }
}
