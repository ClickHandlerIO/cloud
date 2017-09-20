package move.action

import io.netty.buffer.ByteBuf
import move.Wire

/**
 *
 */
abstract sealed class PresenceEvent {
   companion object {
      fun <T : PresenceEvent> pack(msg: T, buffer: ByteBuf): ByteBuf {
         Wire.pack(msg, buffer)
         return buffer
      }

      fun <T : PresenceEvent> unpack(cls: Class<T>, byteBuf: ByteBuf): T {
         return Wire.unpack(cls, byteBuf)
      }
   }
}

sealed class PresenceGetMsg() : PresenceEvent()

sealed class PresenceJoinMsg(val userId: String) : PresenceEvent()

sealed class PresenceLeaveMsg(val id: String, val mod: Long, val userId: String) : PresenceEvent()

sealed class PresenceJoinedMsg(val id: String, val mod: Long) : PresenceEvent()

sealed class PresenceRemovedMsg : PresenceEvent()

@Actor("Presence")
class PresenceActor : AbstractActorAction<PresenceEvent>() {
   var participants = ArrayList<Unit>()

   suspend override fun process(msg: PresenceEvent) {

      when (msg) {
         is PresenceGetMsg -> {
         }
         is PresenceJoinMsg -> {
         }
         is PresenceLeaveMsg -> {
         }
         else -> {
         }
      }
   }

   suspend fun handle(msg: PresenceGetMsg) {

   }

   suspend fun handle(msg: PresenceJoinMsg) {}

   suspend fun handle(msg: PresenceLeaveMsg) {}
}