package io.clickhandler.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 */
public class ActorActionSerializerImpl implements ActorActionSerializer {
    @Override
    public byte[] byteify(Object value) {
        return WireFormat.byteify(value);
    }

    @Override
    public <T> T parse(Class<T> type, byte[] data) {
        return WireFormat.parse(data, type);
    }

    @Override
    public <T> T parse(Class<T> type, byte[] data, int offset, int length) {
        return WireFormat.parse(data, offset, length, type);
    }

    /**
     *
     */
    static class WireFormat {
        public static final Logger LOG = LoggerFactory.getLogger(WireFormat.class);
        public static final ObjectMapper MAPPER = new ObjectMapper();

        static {
            MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            MAPPER.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
            MAPPER.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
            MAPPER.configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, true);

            MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
            MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            MAPPER.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, true);
            MAPPER.configure(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS, true);
        }

        public static <T> T parse(String json, Class<T> cls) {
            if (json == null || json.isEmpty()) {
                return null;
            }
            try {
                return MAPPER.readValue(json, cls);
            } catch (IOException e) {
                LOG.info("Failed to parse Json string", e);
                throw new RuntimeException(e);
            }
        }

        public static <T> T parse(byte[] json, Class<T> cls) {
            if (json == null || json.length == 0) {
                return null;
            }
            try {
                return MAPPER.readValue(json, cls);
            } catch (IOException e) {
                LOG.info("Failed to parse Json data", e);
                throw new RuntimeException(e);
            }
        }

        public static <T> T parse(byte[] json, int offset, int length, Class<T> cls) {
            if (length == 0) {
                return null;
            }
            try {
                return MAPPER.readValue(json, offset, length, cls);
            } catch (IOException e) {
                LOG.info("Failed to parse Json data", e);
                throw new RuntimeException(e);
            }
        }

        public static <T> T parse(InputStream json, Class<T> cls) {
            try {
                return MAPPER.readValue(json, cls);
            } catch (IOException e) {
                LOG.info("Failed to parse Json data", e);
                throw new RuntimeException(e);
            }
        }

        public static byte[] byteify(Object obj) {
            try {
                return MAPPER.writeValueAsBytes(obj);
            } catch (JsonProcessingException e) {
                LOG.info("Failed to byteify Object", e);
                throw new RuntimeException(e);
            }
        }

        public static String stringify(Object obj) {
            try {
                return MAPPER.writeValueAsString(obj);
            } catch (JsonProcessingException e) {
                LOG.info("Failed to stringify Object", e);
                throw new RuntimeException(e);
            }
        }
    }
}
