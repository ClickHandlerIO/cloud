package move.action.inventory

import move.action.Internal
import move.action.InternalAction
import move.action.Move

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
      Move.inventory.SearchStock ask ""
   }
}
