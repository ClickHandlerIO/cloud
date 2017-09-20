package move.action.inventory

import move.action.A
import move.action.Internal
import move.action.InternalAction

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
class SearchJob : InternalAction<String, Unit>() {
   suspend override fun execute() {
      A.inventory.SearchStock ask ""
   }
}
