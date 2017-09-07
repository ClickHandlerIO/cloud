package move.action

import io.netty.util.HashedWheelTimer
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

//@Internal(timeout = 1000)
//class AnotherAction : InternalAction<String, String>() {
//   suspend override fun execute(): String {
////      delay(50)
////      val reply = of(AllocateInventory::class)
////         .await(AllocateInventory.Request(id = ""))
////
////      val reply2 = of<AllocateInventory>() await AllocateInventory.Request(id = "")
////
////      val r = of(AllocateInventory::class)..AllocateInventory.Request(id = "")
//
//      return ""
//   }
//}
//
//@Internal(timeout = 1000)
//class AnotherAction2 : InternalAction<String, String>() {
//   suspend override fun execute(): String {
////      delay(50)
////      val reply = of(AllocateInventory::class)
////         .await(AllocateInventory.Request(id = ""))
////
////      val reply2 = of<AllocateInventory>() await AllocateInventory.Request(id = "")
////
////      val r = of(AllocateInventory::class)..AllocateInventory.Request(id = "")
//
//      return ""
//   }
//}

//@Internal(deadline = 1000)
//class AnotherAction3 : InternalAction<String, String>() {
//   suspend override fun execute(): String {
////      delay(50)
////      val reply = of(AllocateInventory::class)
////         .await(AllocateInventory.Request(id = ""))
////
////      val reply2 = of<AllocateInventory>() await AllocateInventory.Request(id = "")
////
////      val r = of(AllocateInventory::class)..AllocateInventory.Request(id = "")
//
//      return ""
//   }
//}

fun hi() {
   val hashedWheel = HashedWheelTimer(250, TimeUnit.MILLISECONDS)
   hashedWheel.newTimeout({}, 2, TimeUnit.SECONDS)
}