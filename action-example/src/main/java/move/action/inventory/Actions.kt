package move.action.inventory

import move.action.Internal
import move.action.InternalAction
import move.action.Move

//data class LoginRequest(val username: String, val email: String)
data class LoginRequest(var username: String = "")


data class LoginReply(val code: Code = Code.SUCCESS) {
}

enum class Code {
   SUCCESS,
}

/**
 *
 */
@Internal
class Login : InternalAction<LoginRequest, LoginReply>() {
   suspend override fun execute(): LoginReply {

      return LoginReply(Code.SUCCESS)
   }
}


/**
 *
 */
@Internal
class Login2 : InternalAction<LoginRequest, LoginReply>() {
   suspend override fun execute(): LoginReply {
      val reply = Move.inventory.Login3 ask {
         username = ""
      }

      return LoginReply(Code.SUCCESS)
   }
}



/**
 *
 */
@Internal
class Login3 : InternalAction<LoginRequest, LoginReply>() {
   suspend override fun execute(): LoginReply {
//      Move.AllocateInventory ask {
//
//      }

      return LoginReply(Code.SUCCESS)
   }
}