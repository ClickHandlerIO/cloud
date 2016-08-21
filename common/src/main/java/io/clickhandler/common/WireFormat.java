package io.clickhandler.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javaslang.collection.List;
import javaslang.jackson.datatype.JavaslangModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 */
public class WireFormat {
    public static final Logger LOG = LoggerFactory.getLogger(WireFormat.class);
    public static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.registerModule(new JavaslangModule());
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        MAPPER.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        MAPPER.configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, true);

        MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        MAPPER.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, true);
        MAPPER.configure(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS, true);
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static void main(String[] args) {
        System.out.println(WireFormat.stringify(new MyMessage()));
    }

    public static <T> T parse(Class<T> cls, String json) {
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

    public static <T> T parse(Class<T> cls, byte[] json) {
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

    public static <T> T parse(Class<T> cls, byte[] json, int offset, int length) {
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

    public static <T> T parse(Class<T> cls, InputStream json) {
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

    public static class MyMessage {
        @JsonProperty
        List<String> myList = List.of("1", "2", "3");
    }
}