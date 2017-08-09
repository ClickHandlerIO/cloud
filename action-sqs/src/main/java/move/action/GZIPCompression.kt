package move.action

import com.google.common.io.BaseEncoding
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**

 */
object GZIPCompression {
   @Throws(IOException::class)
   fun compress(str: String?): ByteArray? {
      if (str == null || str.length == 0) {
         return null
      }
      val obj = ByteArrayOutputStream()
      val gzip = GZIPOutputStream(obj)
      gzip.write(str.toByteArray(charset("UTF-8")))
      gzip.flush()
      gzip.close()
      return obj.toByteArray()
   }

   @Throws(IOException::class)
   fun compress(str: ByteArray?): ByteArray {
      if (str == null || str.size == 0) {
         return ByteArray(0)
      }
      val obj = ByteArrayOutputStream()
      val gzip = GZIPOutputStream(obj)
      gzip.write(str)
      gzip.flush()
      gzip.close()
      return obj.toByteArray()
   }

   @Throws(IOException::class)
   fun decompress(compressed: ByteArray?): String {
      if (compressed == null || compressed.isEmpty()) {
         return ""
      }
      if (isCompressed(compressed)) {
         val outStr = StringBuilder()
         val gis = GZIPInputStream(ByteArrayInputStream(compressed))
         val bufferedReader = BufferedReader(InputStreamReader(gis, StandardCharsets.UTF_8))

         var line: String? = bufferedReader.readLine()
         while (line != null) {
            outStr.append(line)
            line = bufferedReader.readLine()
         }

         return outStr.toString()
      } else {
         return String(compressed, StandardCharsets.UTF_8)
      }
   }

   @Throws(IOException::class)
   fun pack(str: String?): String? {
      if (str == null || str.isEmpty()) {
         return null
      }
      val obj = ByteArrayOutputStream()
      val gzip = GZIPOutputStream(obj)
      gzip.write(str.toByteArray(charset("UTF-8")))
      gzip.flush()
      gzip.close()
      return BaseEncoding.base64().encode(obj.toByteArray())
   }

   @Throws(IOException::class)
   fun unpack(encoded: String): String {
      val compressed = BaseEncoding.base64().decode(encoded)
      val outStr = StringBuilder()
      if (compressed == null || compressed.size == 0) {
         return ""
      }
      if (isCompressed(compressed)) {
         val gis = GZIPInputStream(ByteArrayInputStream(compressed))
         val bufferedReader = BufferedReader(InputStreamReader(gis, "UTF-8"))

         var line: String? = bufferedReader.readLine()
         while (line != null) {
            outStr.append(line)
            line = bufferedReader.readLine()
         }
         return outStr.toString()
      } else {
         return String(compressed, StandardCharsets.UTF_8)
      }
   }

   fun isCompressed(compressed: ByteArray): Boolean {
      return compressed[0] == GZIPInputStream.GZIP_MAGIC.toByte() && compressed[1] == (GZIPInputStream.GZIP_MAGIC shr 8).toByte()
   }
}