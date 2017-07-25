package move.action

/**

 */
class WorkerRequest(var actionProvider: WorkerActionProvider<Action<Any, Boolean>, Any>? = null,
                    var delaySeconds: Int = 0,
                    var groupId: String? = null,
                    var request: Any? = null) {



   fun actionProvider(actionProvider: WorkerActionProvider<Action<Any, Boolean>, Any>): WorkerRequest {
      this.actionProvider = actionProvider
      return this
   }

   fun delaySeconds(delaySeconds: Int): WorkerRequest {
      this.delaySeconds = delaySeconds
      return this
   }

   fun groupId(groupId: String): WorkerRequest {
      this.groupId = groupId
      return this
   }

   fun request(request: Any): WorkerRequest {
      this.request = request
      return this
   }
}
