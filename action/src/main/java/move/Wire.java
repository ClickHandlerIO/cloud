package move;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.ion.IonObjectMapper;
import com.fasterxml.jackson.dataformat.protobuf.ProtobufFactory;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.vertx.core.buffer.Buffer;
import java.io.DataOutput;
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
public class Wire {

  public static final Logger LOG = LoggerFactory.getLogger(Wire.class);
  public static final ObjectMapper MAPPER = new ObjectMapper();
  public static final ObjectMapper MSGPACK_MAPPER = new ObjectMapper(new MessagePackFactory());
  public static final ObjectMapper CBOR_MAPPER = new ObjectMapper(new CBORFactory());
  public static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

  static {
    configure(MAPPER);
    configure(MSGPACK_MAPPER);
    configure(CBOR_MAPPER);
    configure(YAML_MAPPER);
  }

  static void configure(ObjectMapper mapper) {
    mapper.registerModule(new JavaTimeModule());
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new KotlinModule());
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
    mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
    mapper.configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, true);
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    mapper.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, true);
    mapper.configure(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS, true);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

  public static void main(String[] args) {
    System.out.println(Wire.stringify(new MyMessage()));

  }

  public static <T> T clone(T value) {
    if (value == null) {
      return null;
    }

    return parse((Class<T>) value.getClass(), byteify(value));
  }


  /**
   *
   * @param cls
   * @param data
   * @param <T>
   * @return
   */
  public static <T> T load(Class<T> cls, byte[] data) {
    if (data == null) {
      return null;
    }

    try {
      return CBOR_MAPPER.readValue(data, cls);
    } catch (Throwable e) {
      throw new WireException(e);
    }
  }

  public static <T> T load(Class<T> cls, Buffer buffer) {
    return unpack(cls, buffer.getByteBuf().nioBuffer());
  }

  public static <T> T load(Class<T> cls, ByteBuf buffer) {
    return unpack(cls, buffer.nioBuffer());
  }

  public static <T> T load(Class<T> cls, ByteBuffer buffer) {
    if (buffer == null) {
      return null;
    }

    try {
      return CBOR_MAPPER.readValue(new ByteBufferBackedInputStream(buffer), cls);
    } catch (Throwable e) {
      throw new WireException(e);
    }
  }

  public static byte[] dump(Object obj) {
    try {
      return CBOR_MAPPER.writeValueAsBytes(obj);
    } catch (Throwable e) {
      throw new WireException(e);
    }
  }

  public static void dump(Object obj, ByteBuf buf) {
    try {
      final DataOutput out = new ByteBufOutputStream(buf);
      CBOR_MAPPER.writeValue(out, obj);
    } catch (Throwable e) {
      throw new WireException(e);
    }
  }


  public static <T> T unpack(Class<T> cls, byte[] data) {
    if (data == null) {
      return null;
    }

    try {
      return MSGPACK_MAPPER.readValue(data, cls);
    } catch (Throwable e) {
      throw new WireException(e);
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
      throw new WireException(e);
    }
  }

  public static byte[] pack(Object obj) {
    try {
      return MSGPACK_MAPPER.writeValueAsBytes(obj);
    } catch (Throwable e) {
      throw new WireException(e);
    }
  }

  public static void pack(Object obj, ByteBuf buf) {
    try {
      final DataOutput out = new ByteBufOutputStream(buf);
      MSGPACK_MAPPER.writeValue(out, obj);
    } catch (Throwable e) {
      throw new WireException(e);
    }
  }



  public static <T> T parse(Class<T> cls, String json) {
    if (json == null || json.isEmpty()) {
      return null;
    }
    try {
      return MAPPER.readValue(json, cls);
    } catch (Throwable e) {
      throw new WireException(e);
    }
  }

  public static <T> T parse(Class<T> cls, byte[] json) {
    if (json == null || json.length == 0) {
      return null;
    }
    try {
      return MAPPER.readValue(json, cls);
    } catch (Throwable e) {
      throw new WireException(e);
    }
  }

  public static <T> T parse(Class<T> cls, byte[] json, int offset, int length) {
    if (length == 0) {
      return null;
    }
    try {
      return MAPPER.readValue(json, offset, length, cls);
    } catch (Throwable e) {
      throw new WireException(e);
    }
  }

  public static <T> T parse(Class<T> cls, InputStream json) {
    try {
      return MAPPER.readValue(json, cls);
    } catch (Throwable e) {
      throw new WireException(e);
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
      throw new WireException(e);
    }
  }

  public static byte[] byteify(Object obj) {
    try {
      return MAPPER.writeValueAsBytes(obj);
    } catch (Throwable e) {
      throw new WireException(e);
    }
  }

  public static void byteify(Object obj, ByteBuf buf) {
    try {
      final DataOutput out = new ByteBufOutputStream(buf);
      MAPPER.writeValue(out, obj);
    } catch (Throwable e) {
      throw new WireException(e);
    }
  }

  public static String stringify(Object obj) {
    try {
      return MAPPER.writeValueAsString(obj);
    } catch (Throwable e) {
      throw new WireException(e);
    }
  }


  public static <T> T parseYAML(Class<T> cls, String yaml) {
    if (yaml == null || yaml.isEmpty()) {
      return null;
    }
    try {
      return YAML_MAPPER.readValue(yaml, cls);
    } catch (Throwable e) {
      throw new WireException(e);
    }
  }

  public static <T> T parseYAML(Class<T> cls, byte[] yaml) {
    if (yaml == null || yaml.length == 0) {
      return null;
    }
    try {
      return YAML_MAPPER.readValue(yaml, cls);
    } catch (Throwable e) {
      throw new WireException(e);
    }
  }

  public static <T> T readYAML(Class<T> cls, String yaml) {
    if (yaml == null || yaml.isEmpty()) {
      return null;
    }
    try {
      return YAML_MAPPER.readValue(yaml, cls);
    } catch (Throwable e) {
      throw new WireException(e);
    }
  }

  public static String writeYAML(Object obj) {
    try {
      return YAML_MAPPER.writeValueAsString(obj);
    } catch (Throwable e) {
      throw new WireException(e);
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