package move.action

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import move.common.WireFormat
import javax.inject.Inject

/**
 *
 */
object Net {
   @JvmStatic
   fun main(args: Array<String>) {
      val s = WireFormat.stringify(Message2("Hi", "Description"))
      val u = WireFormat.parse(Message2::class.java, s)

      val uu = WireFormat.parse(Message2::class.java, "{\"name\": null, \"d\": \"D\"}")

      println(s)
   }

   class Worker(var name: String, var max: Int, var maxMem: Long, var rem: Int, var availMem: Long) {
      var sort: Long = 0
         get() = maxMem / rem + availMem
   }

//   @Provides
//   fun message2(): Message2 {
//      return Message2()
//   }

   data class Message2(val name: String = "",
                       @JsonProperty("d") val desc: String? = null)

   class Message @Inject @JsonCreator constructor(val name: String, val description: String?) {
   }

   class MyService @Inject constructor(val msg2: Message2) {
      fun run() {
         println(msg2.desc)
      }
   }
}

object App {
   @JvmStatic
   fun main(args: Array<String>) {
      DaggerApplicationComponent.create().myService().run()
   }
}