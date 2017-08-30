package move.action

import com.google.common.io.BaseEncoding
import org.msgpack.core.MessagePack
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream


data class WorkerEnvelope(val option: Int = 0,
                          val timestamp: Long = 0,
                          val delaySeconds: Int = 0,
                          val name: String = "",
                          val size: Int = 0,
                          val body: ByteArray)

private val EMPTY_BYTE_ARRAY = ByteArray(0)

data class WorkerEnvelope2(
   // UID of message.
   val id: String,
   // Group or Partition ID.
   val groupId: String? = null,
   // De-Duplication ID. Note that it only De-Duplicates within
   // the scope of the Group ID.
   val deduplicationId: String? = null,
   // Topic to send reply to.
   val replyTopic: String? = null,
   val option: Int = 0,
   // Unix Millisecond Epoch
   val created: Long = 0,
   // Delay in seconds. Default to no delay.
   val delay: Int = 0,
   // Name or Type of message
   val name: String? = null,
   // Number of times this message was delivered
   // and failed process
   val deliveries: Int = 0,
   // Milliseconds before visibility timesout
   val timeout: Int = 0,
   // Raw payload bytes
   val body: ByteArray = EMPTY_BYTE_ARRAY
)

/**
 *
 */
object WorkerPacker {
   val COMPRESSION_MIN = 384
   val NULL_ENVELOPE = WorkerEnvelope(0, 0L, 0, "", 0, ByteArray(0))

   fun stringify(envelope: WorkerEnvelope): String {
      return BaseEncoding.base64().encode(byteify(envelope, true))
   }

   fun byteify(envelope: WorkerEnvelope, compress: Boolean = false): ByteArray {
      val packer = MessagePack.newDefaultBufferPacker()
      try {
         packer.packInt(envelope.option)
         packer.packLong(envelope.timestamp)
         packer.packInt(envelope.delaySeconds)
         packer.packString(envelope.name)
         packer.packInt(envelope.body.size)
         packer.addPayload(envelope.body)
         val buffer = packer.toByteArray()

         if (!compress || buffer.size < COMPRESSION_MIN) {
            return buffer
         } else {
            return compress(buffer)
         }
      } finally {
         packer.close()
      }
   }

   fun parse(str: String): WorkerEnvelope {
      if (str.isNullOrBlank()) {
         return NULL_ENVELOPE
      }

      return parse(unpack(BaseEncoding.base64().decode(str)))
   }

   fun parseCompressed(buffer: ByteArray): WorkerEnvelope {
      return parse(unpack(buffer))
   }

   fun parse(buffer: ByteArray): WorkerEnvelope {
      val unpacker = MessagePack.newDefaultUnpacker(buffer)

      try {
         if (!unpacker.hasNext())
            return NULL_ENVELOPE

         val option = unpacker.unpackInt()
         if (!unpacker.hasNext())
            return NULL_ENVELOPE

         val timestamp = unpacker.unpackLong()
         if (!unpacker.hasNext())
            return NULL_ENVELOPE

         val delaySeconds = unpacker.unpackInt()
         if (!unpacker.hasNext())
            return NULL_ENVELOPE

         val name = unpacker.unpackString()
         if (!unpacker.hasNext())
            return NULL_ENVELOPE

         val size = unpacker.unpackInt()

         return WorkerEnvelope(
            option,
            timestamp,
            delaySeconds,
            name,
            size,
            if (size <= 0)
               ByteArray(0)
            else
               unpacker.readPayload(size)
         )
      } finally {
         unpacker.close()
      }
   }

   @JvmStatic
   fun main(args: Array<String>) {

      val payload0 = "1"//"{\"doc\":{\"orgId\":\"a1acf243e8dc44f3bbb734327b40cd04\",\"orgUnitId\":\"ab6542f861b7476cae96ef65f19532f7\",\"generatedByUserId\":\"20133faf53554c2eb6cf21551e7517ff\",\"docReportType\":\"SALES_ORDER_PO_REQUEST\",\"format\":\"PDF\",\"displayType\":\"WEB\",\"requestClassName\":\"action.worker.docreport.order.GenerateSalesOrderPORequest\",\"parameters\":\"{\\\"orderId\\\":\\\"544c71a4631a430582f56c54aa8def0e\\\"}\",\"startDate\":\"2017-08-08T19:43:01.109Z\",\"endDate\":null,\"processingTimeSeconds\":0.0,\"expiresOnDate\":\"2017-08-09T19:43:01.109Z\",\"status\":\"PENDING\",\"timeout\":\"2017-08-08T20:43:01.109Z\",\"attempt\":1,\"maxDownloads\":5,\"v\":0,\"id\":\"9e859f44280a4501a15126d08fffe915\"},\"orderId\":\"544c71a4631a430582f56c54aa8def0e\"}{\"doc\":{\"orgId\":\"a1acf243e8dc44f3bbb734327b40cd04\",\"orgUnitId\":\"ab6542f861b7476cae96ef65f19532f7\",\"generatedByUserId\":\"20133faf53554c2eb6cf21551e7517ff\",\"docReportType\":\"SALES_ORDER_PO_REQUEST\",\"format\":\"PDF\",\"displayType\":\"WEB\",\"requestClassName\":\"action.worker.docreport.order.GenerateSalesOrderPORequest\",\"parameters\":\"{\\\"orderId\\\":\\\"544c71a4631a430582f56c54aa8def0e\\\"}\",\"startDate\":\"2017-08-08T19:43:01.109Z\",\"endDate\":null,\"processingTimeSeconds\":0.0,\"expiresOnDate\":\"2017-08-09T19:43:01.109Z\",\"status\":\"PENDING\",\"timeout\":\"2017-08-08T20:43:01.109Z\",\"attempt\":1,\"maxDownloads\":5,\"v\":0,\"id\":\"9e859f44280a4501a15126d08fffe915\"},\"orderId\":\"544c71a4631a430582f56c54aa8def0e\"}"

      val payload = payload0.toByteArray(StandardCharsets.UTF_8)
//         val payload = "{\"doc\":{\"orgId\":\"a1acf243e8dc44f3bbb734327b40cd04\",\"orgUnitId\":\"ab6542f861b7476cae96ef65f19532f7\",\"generatedByUserId\":\"20133faf53554c2eb6cf21551e7517ff\",\"docReportType\":\"SALES_ORDER_PO_REQUEST\",\"format\":\"PDF\",\"displayType\":\"WEB\",\"requestClassName\":\"action.worker.docreport.order.GenerateSalesOrderPORequest\",\"parameters\":\"{\\\"orderId\\\":\\\"544c71a4631a430582f56c54aa8def0e\\\"}\",\"startDate\":\"2017-08-08T19:43:01.109Z\",\"endDate\":null,\"processingTimeSeconds\":0.0,\"expiresOnDate\":\"2017-08-09T19:43:01.109Z\",\"status\":\"PENDING\",\"timeout\":\"2017-08-08T20:43:01.109Z\",\"attempt\":1,\"maxDownloads\":5,\"v\":0,\"id\":\"9e859f44280a4501a15126d08fffe915\"},\"orderId\":\"544c71a4631a430582f56c54aa8def0e\"}/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/bin/java -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:51853,suspend=y,server=n -Dfile.encoding=UTF-8 -classpath \"/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/charsets.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/deploy.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/ext/cldrdata.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/ext/dnsns.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/ext/jaccess.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/ext/jfxrt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/ext/localedata.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/ext/nashorn.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/ext/sunec.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/ext/sunjce_provider.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/ext/sunpkcs11.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/ext/zipfs.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/javaws.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/jce.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/jfr.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/jfxswt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/jsse.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/management-agent.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/plugin.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/resources.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/jre/lib/rt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/lib/ant-javafx.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/lib/dt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/lib/javafx-mx.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/lib/jconsole.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/lib/packager.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/lib/sa-jdi.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_141.jdk/Contents/Home/lib/tools.jar:/Users/clay/repos/move/cloud/action-sqs/target/classes:/Users/clay/.m2/repository/com/google/dagger/dagger/2.11/dagger-2.11.jar:/Users/clay/.m2/repository/javax/inject/javax.inject/1/javax.inject-1.jar:/Users/clay/.m2/repository/org/jetbrains/kotlin/kotlin-stdlib-jre8/1.1.3-2/kotlin-stdlib-jre8-1.1.3-2.jar:/Users/clay/.m2/repository/org/jetbrains/kotlin/kotlin-stdlib/1.1.3-2/kotlin-stdlib-1.1.3-2.jar:/Users/clay/.m2/repository/org/jetbrains/annotations/13.0/annotations-13.0.jar:/Users/clay/.m2/repository/org/jetbrains/kotlin/kotlin-stdlib-jre7/1.1.3-2/kotlin-stdlib-jre7-1.1.3-2.jar:/Users/clay/repos/move/cloud/action/target/classes:/Users/clay/repos/move/cloud/common/target/classes:/Users/clay/.m2/repository/org/jetbrains/kotlinx/kotlinx-coroutines-core/0.16/kotlinx-coroutines-core-0.16.jar:/Users/clay/.m2/repository/org/jetbrains/kotlinx/kotlinx-coroutines-rx1/0.16/kotlinx-coroutines-rx1-0.16.jar:/Users/clay/.m2/repository/io/javaslang/javaslang/2.0.4/javaslang-2.0.4.jar:/Users/clay/.m2/repository/io/javaslang/javaslang-match/2.0.4/javaslang-match-2.0.4.jar:/Users/clay/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.8.9/jackson-core-2.8.9.jar:/Users/clay/.m2/repository/com/fasterxml/jackson/core/jackson-databind/2.8.9/jackson-databind-2.8.9.jar:/Users/clay/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.8.9/jackson-annotations-2.8.9.jar:/Users/clay/.m2/repository/com/fasterxml/jackson/datatype/jackson-datatype-jsr310/2.8.9/jackson-datatype-jsr310-2.8.9.jar:/Users/clay/.m2/repository/com/fasterxml/jackson/datatype/jackson-datatype-jdk8/2.8.9/jackson-datatype-jdk8-2.8.9.jar:/Users/clay/.m2/repository/com/fasterxml/jackson/datatype/jackson-datatype-guava/2.8.9/jackson-datatype-guava-2.8.9.jar:/Users/clay/.m2/repository/com/hazelcast/hazelcast-all/3.8.3/hazelcast-all-3.8.3.jar:/Users/clay/.m2/repository/io/vertx/vertx-core/3.4.2/vertx-core-3.4.2.jar:/Users/clay/.m2/repository/io/netty/netty-common/4.1.8.Final/netty-common-4.1.8.Final.jar:/Users/clay/.m2/repository/io/netty/netty-buffer/4.1.8.Final/netty-buffer-4.1.8.Final.jar:/Users/clay/.m2/repository/io/netty/netty-transport/4.1.8.Final/netty-transport-4.1.8.Final.jar:/Users/clay/.m2/repository/io/netty/netty-handler/4.1.8.Final/netty-handler-4.1.8.Final.jar:/Users/clay/.m2/repository/io/netty/netty-codec/4.1.8.Final/netty-codec-4.1.8.Final.jar:/Users/clay/.m2/repository/io/netty/netty-handler-proxy/4.1.8.Final/netty-handler-proxy-4.1.8.Final.jar:/Users/clay/.m2/repository/io/netty/netty-codec-socks/4.1.8.Final/netty-codec-socks-4.1.8.Final.jar:/Users/clay/.m2/repository/io/netty/netty-codec-http/4.1.8.Final/netty-codec-http-4.1.8.Final.jar:/Users/clay/.m2/repository/io/netty/netty-codec-http2/4.1.8.Final/netty-codec-http2-4.1.8.Final.jar:/Users/clay/.m2/repository/io/netty/netty-resolver/4.1.8.Final/netty-resolver-4.1.8.Final.jar:/Users/clay/.m2/repository/io/netty/netty-resolver-dns/4.1.8.Final/netty-resolver-dns-4.1.8.Final.jar:/Users/clay/.m2/repository/io/netty/netty-codec-dns/4.1.8.Final/netty-codec-dns-4.1.8.Final.jar:/Users/clay/.m2/repository/io/vertx/vertx-web/3.4.2/vertx-web-3.4.2.jar:/Users/clay/.m2/repository/io/vertx/vertx-auth-common/3.4.2/vertx-auth-common-3.4.2.jar:/Users/clay/.m2/repository/io/vertx/vertx-rx-java/3.4.2/vertx-rx-java-3.4.2.jar:/Users/clay/.m2/repository/io/vertx/vertx-hazelcast/3.4.2/vertx-hazelcast-3.4.2.jar:/Users/clay/.m2/repository/io/vertx/vertx-dropwizard-metrics/3.4.2/vertx-dropwizard-metrics-3.4.2.jar:/Users/clay/.m2/repository/com/google/guava/guava/21.0/guava-21.0.jar:/Users/clay/.m2/repository/org/reflections/reflections/0.9.11/reflections-0.9.11.jar:/Users/clay/.m2/repository/org/javassist/javassist/3.21.0-GA/javassist-3.21.0-GA.jar:/Users/clay/.m2/repository/javax/validation/validation-api/1.1.0.Final/validation-api-1.1.0.Final.jar:/Users/clay/.m2/repository/joda-time/joda-time/2.9.4/joda-time-2.9.4.jar:/Users/clay/.m2/repository/io/dropwizard/metrics/metrics-core/3.2.3/metrics-core-3.2.3.jar:/Users/clay/.m2/repository/io/dropwizard/metrics/metrics-healthchecks/3.2.3/metrics-healthchecks-3.2.3.jar:/Users/clay/.m2/repository/io/dropwizard/metrics/metrics-graphite/3.2.3/metrics-graphite-3.2.3.jar:/Users/clay/.m2/repository/io/vertx/vertx-circuit-breaker/3.4.2/vertx-circuit-breaker-3.4.2.jar:/Users/clay/.m2/repository/com/netflix/hystrix/hystrix-core/1.5.12/hystrix-core-1.5.12.jar:/Users/clay/.m2/repository/org/slf4j/slf4j-api/1.7.24/slf4j-api-1.7.24.jar:/Users/clay/.m2/repository/com/netflix/archaius/archaius-core/0.4.1/archaius-core-0.4.1.jar:/Users/clay/.m2/repository/commons-configuration/commons-configuration/1.8/commons-configuration-1.8.jar:/Users/clay/.m2/repository/commons-lang/commons-lang/2.6/commons-lang-2.6.jar:/Users/clay/.m2/repository/io/reactivex/rxjava/1.3.0/rxjava-1.3.0.jar:/Users/clay/.m2/repository/org/hdrhistogram/HdrHistogram/2.1.9/HdrHistogram-2.1.9.jar:/Users/clay/.m2/repository/com/amazonaws/aws-java-sdk-sqs/1.11.166/aws-java-sdk-sqs-1.11.166.jar:/Users/clay/.m2/repository/com/amazonaws/aws-java-sdk-core/1.11.166/aws-java-sdk-core-1.11.166.jar:/Users/clay/.m2/repository/commons-logging/commons-logging/1.1.3/commons-logging-1.1.3.jar:/Users/clay/.m2/repository/org/apache/httpcomponents/httpclient/4.5.2/httpclient-4.5.2.jar:/Users/clay/.m2/repository/org/apache/httpcomponents/httpcore/4.4.4/httpcore-4.4.4.jar:/Users/clay/.m2/repository/commons-codec/commons-codec/1.9/commons-codec-1.9.jar:/Users/clay/.m2/repository/software/amazon/ion/ion-java/1.0.2/ion-java-1.0.2.jar:/Users/clay/.m2/repository/com/fasterxml/jackson/dataformat/jackson-dataformat-cbor/2.6.7/jackson-dataformat-cbor-2.6.7.jar:/Users/clay/.m2/repository/com/amazonaws/jmespath-java/1.11.166/jmespath-java-1.11.166.jar:/Users/clay/.m2/repository/com/amazonaws/aws-java-sdk-s3/1.11.166/aws-java-sdk-s3-1.11.166.jar:/Users/clay/.m2/repository/com/amazonaws/aws-java-sdk-kms/1.11.166/aws-java-sdk-kms-1.11.166.jar:/Users/clay/.m2/repository/com/squareup/javapoet/1.9.0/javapoet-1.9.0.jar:/Applications/IntelliJ IDEA.app/Contents/lib/idea_rt.jar\" move.action.SQSService\n" +
//            "Connected to the target VM, address: '127.0.0.1:51853', transport: 'socket'\n" +
//            "SLF4J: Failed to load class \"org.slf4j.impl.StaticLoggerBinder\".\n" +
//            "SLF4J: Defaulting to no-operation (NOP) logger implementation\n" +
//            "SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.\n" +
//            "Raw: 692   Packed: 648   FastLZ: 587    FastLZ-Packed: 784      GZIP: 462\n" +
//            "Disconnected from the target VM, address: '127.0.0.1:51853', transport: 'socket'\n" +
//            "01000|move.MyAction|{}\n" +
//            "11000|move.MyAction|{}\n" +
//            "SQSEnvelope(l=false, s=1000, name=move.MyAction, p={})\n" +
//            "\n" +
//            "Process finished with exit code 0"
      val uncompressed = byteify(WorkerEnvelope(0, System.currentTimeMillis(), 0, "MyAction", payload.size, payload), false)
      val compressed = byteify(WorkerEnvelope(0, System.currentTimeMillis(), 0, "MyAction", payload.size, payload), true)
      val compressedString = stringify(WorkerEnvelope(0, System.currentTimeMillis(), 0, "MyAction", payload.size, payload))

      println("Uncompressed: " + uncompressed.size + "   Compressed: " + compressed.size + "   Compressed Base64: " + compressedString.length)

      val unpacked = parse(compressedString)

//         val envelopeSerialized = stringify(WorkerEnvelope(false, 1000, "move.MyAction", "{}"))
//         println(stringify(SQSEnvelope(false, 1000, "move.MyAction", "{}")))
//         println(stringify(SQSEnvelope(true, 1000, "move.MyAction", "{}")))


      println(unpacked)
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
   fun decompress(compressed: ByteArray?): ByteArray {
      if (compressed == null || compressed.isEmpty()) {
         return ByteArray(0)
      }
      if (!isCompressed(compressed)) {
         return compressed
      }
      val gis = GZIPInputStream(ByteArrayInputStream(compressed))
      val out = ByteArrayOutputStream()

      val buffer = ByteArray(8192)
      var length = gis.read(buffer)
      while (length > -1) {
         out.write(buffer, 0, length)
         length = gis.read(buffer)
      }

      gis.close()
      return out.toByteArray()
   }

   @Throws(IOException::class)
   fun unpack(compressed: ByteArray?): ByteArray {
      if (compressed == null || compressed.isEmpty()) {
         return ByteArray(0)
      }

      if (!isCompressed(compressed))
         return compressed


      return decompress(compressed)
   }

   fun isCompressed(compressed: ByteArray): Boolean {
      return compressed[0] == GZIPInputStream.GZIP_MAGIC.toByte() && compressed[1] == (GZIPInputStream.GZIP_MAGIC shr 8).toByte()
   }
}