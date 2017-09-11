package move.action

/**

 */
class WorkerRequest(var actionProvider: WorkerActionProvider<WorkerAction<*, *>, *, *>? = null,
                    var delaySeconds: Int = 0,
                    var groupId: String? = null,
                    var request: Any)
