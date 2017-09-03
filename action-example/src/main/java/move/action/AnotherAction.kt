package move.action

@Internal(timeout = 1000)
class AnotherAction : InternalAction<String, String>() {
   suspend override fun execute(): String {
//      delay(50)
//      val reply = of(AllocateInventory::class)
//         .await(AllocateInventory.Request(id = ""))
//
//      val reply2 = of<AllocateInventory>() await AllocateInventory.Request(id = "")
//
//      val r = of(AllocateInventory::class)..AllocateInventory.Request(id = "")

      return ""
   }
}

@Internal(timeout = 1000)
class AnotherAction2 : InternalAction<String, String>() {
   suspend override fun execute(): String {
//      delay(50)
//      val reply = of(AllocateInventory::class)
//         .await(AllocateInventory.Request(id = ""))
//
//      val reply2 = of<AllocateInventory>() await AllocateInventory.Request(id = "")
//
//      val r = of(AllocateInventory::class)..AllocateInventory.Request(id = "")

      return ""
   }
}

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