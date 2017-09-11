package move.action.inventory

import move.action.Internal
import move.action.InternalAction
import move.action.InternalJob

/**
 *
 */
@Internal
class SearchStock : InternalAction<String, String>() {
   suspend override fun execute(): String {
      return "SEARCHED"
   }
}

/**
 *
 */
@Internal
class SearchJob : InternalJob<String>() {
   suspend override fun execute() {
   }
}
