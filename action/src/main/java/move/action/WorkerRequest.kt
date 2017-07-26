package move.action

/**

 */
class WorkerRequest(var actionProvider: WorkerActionProvider<Action<Any, Boolean>, Any>? = null,
                    var delaySeconds: Int = 0,
                    var groupId: String? = null,
                    var request: Any? = null)
