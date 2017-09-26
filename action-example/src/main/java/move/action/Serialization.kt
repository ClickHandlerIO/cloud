package move.action

import move.NUID
import move.Wire
import net.jpountz.lz4.LZ4Factory
import org.xerial.snappy.Snappy
import java.io.File
import java.nio.file.Files

/**
 *
 */
object WireFormats {
   val lz4Factory = LZ4Factory.fastestInstance()

   data class KTestInt32(
      val a: Int,
      val id: String = NUID.nextGlobal(),
      val name: String = "This is a name",
      val address: String = ("message Employee {\n"
         + " required string name = 1;\n"
         + " required int32 age = 2;\n"
         + " repeated string emails = 3;\n"
         + " optional Employee boss = 4;\n"
         + "}\n")
   )

   data class MyRequest(val id: String, val created: Long)

   @JvmStatic
   fun main(args: Array<String>) {
//      val frameBytes = ByteBuffer.wrap(frame.toUtf8Bytes())
//      val frameBytes0 = frame.toUtf8Bytes()
//      val xx64 = XXHashFactory.fastestInstance().hash32()
//
//      val crc16 = CRC16.calc(frameBytes0)
//
//      val frameBytesDirect = ByteBuffer.allocateDirect(frameBytes0.size)
//      frameBytesDirect.put(frameBytes0)
//      frameBytesDirect.flip()
//
//      for (x in 1..30) {
//         var start = System.currentTimeMillis()
//         for (i in 1..1_000_000) {
//            xx64.hash(frameBytes, 0)
//         }
//         println("XX64: ${System.currentTimeMillis() - start}")
//      }
//
//      for (x in 1..30) {
//         var start = System.currentTimeMillis()
//         for (i in 1..1_000_000) {
//            CRC16.calc(frameBytes0)
//         }
//         println("CRC16: ${System.currentTimeMillis() - start}")
//      }

//      Snappy.compress()

      val smallJson = Files.readAllBytes(File("action-example/small.txt").toPath())
      val largeJson = Files.readAllBytes(File("action-example/large.txt").toPath())

//      println("Frame Size: ${frame.toByteArray().size} -> LZ4: ${lz4Factory.fastCompressor().compress(frame.toByteArray()).size}")
      println("Samll Size: ${smallJson.size} -> LZ4: ${lz4Factory.fastCompressor().compress(smallJson).size}  -> Snappy ${Snappy.compress(smallJson).size}")
      println("Large Size: ${largeJson.size} -> LZ4: ${lz4Factory.fastCompressor().compress(largeJson).size}  -> Snappy ${Snappy.compress(largeJson).size}")

      val obj = KTestInt32(12)

      val iterations = 10
//      println()
//      println("Kotlin CBOR")
//      println("CBOR Size: ${CBOR.dump(obj).size}")
//      println("LZ4 Size:  ${lz4Factory.fastCompressor().compress(CBOR.dump(obj)).size}")
//      for (x in 1..iterations) {
//         var start = System.currentTimeMillis()
//         for (i in 1..1_000_000) {
//            val bytes = CBOR.dump(obj)
//         }
//         val serialized = System.currentTimeMillis() - start
//         val bytes = CBOR.dump(obj)
//         start = System.currentTimeMillis()
//         for (i in 1..1_000_000) {
//            CBOR.load<KTestInt32>(bytes)
//         }
//         println("Serialization: ${serialized}  -> Deserialization: ${System.currentTimeMillis() - start}")
//      }


//      println()
//      println("Kotlin Protobuf")
//      println("ProtpBuf Size: ${ProtoBuf.dump(obj).size}")
//      println("LZ4 Size:  ${lz4Factory.fastCompressor().compress(ProtoBuf.dump(obj)).size}")
//      for (x in 1..iterations) {
//         var start = System.currentTimeMillis()
//         for (i in 1..1_000_000) {
//            val bytes = ProtoBuf.dump(obj)
//         }
//         val serialized = System.currentTimeMillis() - start
//         val bytes = ProtoBuf.dump(obj)
//         start = System.currentTimeMillis()
//         for (i in 1..1_000_000) {
//            ProtoBuf.load<KTestInt32>(bytes)
//         }
//         println("Serialization: ${serialized}  -> Deserialization: ${System.currentTimeMillis() - start}")
//      }

//      println()
//      println("Jackson ProtoBuf")
//      for (x in 1..iterations) {
//         var start = System.currentTimeMillis()
//         for (i in 1..1_000_000) {
//            val bytes = Wire.proto(obj)
//         }
//         val serialized = System.currentTimeMillis() - start
//         val bytes = Wire.proto(obj)
//         println("MsgPack Size: ${bytes.size}")
//         start = System.currentTimeMillis()
//         for (i in 1..1_000_000) {
//            Wire.unproto(KTestInt32::class.java, bytes)
//         }
//         println("Serialization: ${serialized}  -> Deserialization: ${System.currentTimeMillis() - start}")
//      }

//      println()
//      println("Jackson Smile")
//      for (x in 1..iterations) {
//         var start = System.currentTimeMillis()
//         for (i in 1..1_000_000) {
//            val bytes = Wire.smile(obj)
//         }
//         val serialized = System.currentTimeMillis() - start
//         val bytes = Wire.smile(obj)
//         println("CBOR Size: ${bytes.size}")
//         start = System.currentTimeMillis()
//         for (i in 1..1_000_000) {
//            Wire.unsmile(KTestInt32::class.java, bytes)
//         }
//         println("Serialization: ${serialized}  -> Deserialization: ${System.currentTimeMillis() - start}")
//      }
//
//      println()
//      println("Jackson ION")
//      for (x in 1..iterations) {
//         var start = System.currentTimeMillis()
//         for (i in 1..1_000_000) {
//            val bytes = Wire.ion(obj)
//         }
//         val serialized = System.currentTimeMillis() - start
//         val bytes = Wire.ion(obj)
//         println("ION Size: ${bytes.size}")
//         start = System.currentTimeMillis()
//         for (i in 1..1_000_000) {
//            Wire.deion(KTestInt32::class.java, bytes)
//         }
//         println("Serialization: ${serialized}  -> Deserialization: ${System.currentTimeMillis() - start}")
//      }

//      println()
//      println("Jackson MsgPack")
//      for (x in 1..iterations) {
//         var start = System.currentTimeMillis()
//         for (i in 1..1_000_000) {
//            val bytes = Wire.pack(obj)
//         }
//         val serialized = System.currentTimeMillis() - start
//         val bytes = Wire.pack(obj)
//         println("MsgPack Size: ${bytes.size}")
//         start = System.currentTimeMillis()
//         for (i in 1..1_000_000) {
//            Wire.unpack(KTestInt32::class.java, bytes)
//         }
//         println("Serialization: ${serialized}  -> Deserialization: ${System.currentTimeMillis() - start}")
//      }

      println()
      println("Jackson CBOR")
      println("CBOR Size: ${Wire.dump(obj).size}")
      println("LZ4 Size:  ${lz4Factory.fastCompressor().compress(Wire.dump(obj)).size}")
      for (x in 1..iterations) {
         var start = System.currentTimeMillis()
         for (i in 1..1_000_000) {
            val bytes = Wire.dump(obj)
         }
         val serialized = System.currentTimeMillis() - start
         val bytes = Wire.dump(obj)
         start = System.currentTimeMillis()
         for (i in 1..1_000_000) {
            Wire.load(KTestInt32::class.java, bytes)
         }
         println("Serialization: ${serialized}  -> Deserialization: ${System.currentTimeMillis() - start}")
      }

//      val back = CBOR.load<KTestInt32>(KTestInt32::class.serializer(), bytes)
//
//      println(bytes)
//      println(back)
   }
}