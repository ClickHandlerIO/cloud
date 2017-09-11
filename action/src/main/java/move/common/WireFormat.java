package move.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.jetbrains.annotations.NotNull;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class WireFormat {

  public static final Logger LOG = LoggerFactory.getLogger(WireFormat.class);
  public static final ObjectMapper MAPPER = new ObjectMapper();
  public static final ObjectMapper MSGPACK_MAPPER = new ObjectMapper(new MessagePackFactory());

  static {
    MAPPER.registerModule(new JavaTimeModule());
    MAPPER.registerModule(new Jdk8Module());
    MAPPER.registerModule(new KotlinModule());

    MSGPACK_MAPPER.registerModule(new JavaTimeModule());
    MSGPACK_MAPPER.registerModule(new Jdk8Module());
    MSGPACK_MAPPER.registerModule(new KotlinModule());

    MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    MAPPER.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
    MAPPER.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
    MAPPER.configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, true);

    MSGPACK_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    MSGPACK_MAPPER.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
    MSGPACK_MAPPER.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
    MSGPACK_MAPPER.configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, true);

    MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    MAPPER.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, true);
    MAPPER.configure(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS, true);
    MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    MSGPACK_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    MSGPACK_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    MSGPACK_MAPPER.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, true);
    MSGPACK_MAPPER.configure(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS, true);
    MSGPACK_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

  public static void main(String[] args) {
    System.out.println(WireFormat.stringify(new MyMessage()));

  }

  public static <T> T clone(T value) {
    if (value == null) {
      return null;
    }

    return parse((Class<T>) value.getClass(), byteify(value));
  }

  public static <T> T unpack(Class<T> cls, byte[] data) {
    if (data == null) {
      return null;
    }

    try {
      return MSGPACK_MAPPER.readValue(data, cls);
    } catch (Throwable e) {
      LOG.info("Failed to unpack MsgPack data", e);
      throw new RuntimeException(e);
    }
  }

  public static <T> T unpack(Class<T> cls, Buffer buffer) {
    return unpack(cls, buffer.getByteBuf().nioBuffer());
  }

  public static <T> T unpack(Class<T> cls, ByteBuf buffer) {
    return unpack(cls, buffer.nioBuffer());
  }

  public static <T> T unpack(Class<T> cls, ByteBuffer buffer) {
    if (buffer == null) {
      return null;
    }

    try {
      return MSGPACK_MAPPER.readValue(new ByteBufferBackedInputStream(buffer), cls);
    } catch (Throwable e) {
      LOG.info("Failed to unpack MsgPack data", e);
      throw new RuntimeException(e);
    }
  }

  public static byte[] pack(Object obj) {
    try {
      return MSGPACK_MAPPER.writeValueAsBytes(obj);
    } catch (Throwable e) {
      LOG.info("Failed to byteify Object", e);
//            throw new RuntimeException(e);
      return null;
    }
  }


  public static <T> T parse(Class<T> cls, String json) {
    if (json == null || json.isEmpty()) {
      return null;
    }
    try {
      return MAPPER.readValue(json, cls);
    } catch (Throwable e) {
      LOG.info("Failed to parse Json string", e);
//            throw new RuntimeException(e);
      return null;
    }
  }

  public static <T> T parse(Class<T> cls, byte[] json) {
    if (json == null || json.length == 0) {
      return null;
    }
    try {
      return MAPPER.readValue(json, cls);
    } catch (Throwable e) {
      LOG.info("Failed to parse Json data", e);
//            throw new RuntimeException(e);
      return null;
    }
  }

  public static <T> T parse(Class<T> cls, byte[] json, int offset, int length) {
    if (length == 0) {
      return null;
    }
    try {
      return MAPPER.readValue(json, offset, length, cls);
    } catch (Throwable e) {
      LOG.info("Failed to parse Json data", e);
//            throw new RuntimeException(e);
      return null;
    }
  }

  public static <T> T parse(Class<T> cls, InputStream json) {
    try {
      return MAPPER.readValue(json, cls);
    } catch (Throwable e) {
      LOG.info("Failed to parse Json data", e);
//            throw new RuntimeException(e);
      return null;
    }
  }

  public static <T> T parse(Class<T> cls, Buffer buffer) {
    return parse(cls, buffer.getByteBuf().nioBuffer());
  }

  public static <T> T parse(Class<T> cls, ByteBuf buffer) {
    return parse(cls, buffer.nioBuffer());
  }

  public static <T> T parse(Class<T> cls, ByteBuffer buffer) {
    if (buffer == null) {
      return null;
    }

    try {
      return MAPPER.readValue(new ByteBufferBackedInputStream(buffer), cls);
    } catch (Throwable e) {
      LOG.info("Failed to unpack MsgPack data", e);
      throw new RuntimeException(e);
    }
  }

  public static byte[] byteify(Object obj) {
    try {
      return MAPPER.writeValueAsBytes(obj);
    } catch (Throwable e) {
      LOG.info("Failed to byteify Object", e);
//            throw new RuntimeException(e);
      return null;
    }
  }

  public static String stringify(Object obj) {
    try {
      return MAPPER.writeValueAsString(obj);
    } catch (Throwable e) {
      LOG.info("Failed to stringify Object", e);
//            throw new RuntimeException(e);
      return null;
    }
  }

  public static class MyMessage {

    @JsonProperty
    LocalDate localDate = LocalDate.now();
    @JsonProperty
    LocalTime localTime = LocalTime.now();
    @JsonProperty
    LocalDateTime localDateTime = LocalDateTime.now();
    @JsonProperty
    ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of("PST", ZoneId.SHORT_IDS));

    @JsonProperty
    Duration duration = Duration.ofDays(2);
    @JsonProperty
    Instant instant = Instant.now(Clock.systemDefaultZone());
  }

  public static class ByteBufferBackedInputStream extends InputStream {

    protected final ByteBuffer _b;

    public ByteBufferBackedInputStream(ByteBuffer buf) {
      _b = buf;
    }

    @Override
    public int available() {
      return _b.remaining();
    }

    @Override
    public int read() throws IOException {
      return _b.hasRemaining() ? (_b.get() & 0xFF) : -1;
    }

    @Override
    public int read(@NotNull byte[] bytes, int off, int len) throws IOException {
      if (!_b.hasRemaining()) {
        return -1;
      }
      len = Math.min(len, _b.remaining());
      _b.get(bytes, off, len);
      return len;
    }
  }

  public static class ByteBufBackedInputStream extends InputStream {

    protected final ByteBuf _b;

    public ByteBufBackedInputStream(ByteBuf buf) {
      _b = buf;
    }

    @Override
    public int available() {
      return _b.readableBytes();
    }

    @Override
    public int read() throws IOException {
      return _b.readableBytes() > 0 ? (_b.readByte() & 0xFF) : -1;
    }

    @Override
    public int read(byte[] bytes, int off, int len) throws IOException {
      if (_b.readableBytes() <= 0) {
        return -1;
      }
      len = Math.min(len, _b.readableBytes());
      _b.readBytes(bytes, off, len);
      return len;
    }
  }
}