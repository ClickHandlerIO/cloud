package move.action

import io.netty.buffer.ByteBuf
import move.Wire
import org.apache.commons.collections4.list.TreeList

/**
 *
 */
abstract sealed class PresenceEvent : ActorMessage() {
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

sealed class PresenceEventSetMsg(val messages: List<PresenceEvent>)

sealed class PresenceGetMsg() : PresenceEvent()

sealed class PresenceJoinMsg(val userId: String) : PresenceEvent()

sealed class PresenceLeaveMsg(val id: String, val mod: Long, val userId: String) : PresenceEvent()

sealed class PresenceJoinedMsg(val id: String, val mod: Long) : PresenceEvent()

sealed class PresenceRemovedMsg : PresenceEvent()

sealed class PresencePushMsg : PresenceEvent()

@Actor("Presence")
class PresenceActor : ActorAction() {
   override val intervalMillis: Long
      get() = 30_000

   // Use array list up to 8
   var participant: Participant? = null
   var participant2: Participant? = null
   var participant3: Participant? = null

   // Use TreeList
   var particpantLargeList: TreeList<Participant>? = null

   suspend override fun onInterval() {
   }

   suspend override fun startUp() {
   }

   suspend override fun shutdown() {
   }

   private fun evict() {
      if (participant == null) {
         particpantLargeList?.forEach { }
      }
   }

   suspend override fun handle(msg: ActorMessage) {
      when (msg) {
         is PresenceEventSetMsg -> {
            msg.messages.forEach { handle(it) }
         }
         is PresenceGetMsg -> {
         }
         is PresenceJoinMsg -> {
         }
         is PresenceLeaveMsg -> {
         }
         is PresencePushMsg -> {
         }
         else -> {
         }
      }
   }

   suspend fun handle(msg: PresenceGetMsg) {
   }

   suspend fun handle(msg: PresenceJoinMsg) {}

   suspend fun handle(msg: PresenceLeaveMsg) {}

   suspend fun handle(msg: PresencePushMsg) {}

   data class Participant(
      val userId: String,
      val joined: Long = System.currentTimeMillis(),
      var lastPing: Long = joined,
      var name: String = "",
      var state: String = ""
   )

   companion object {

   }
}