package com.riton.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.riton.exception.JsonParseException;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class JsonUtils {
    private static ObjectMapper objectMapper = new ObjectMapper();
    private static final Map<Class,TypeReference> referenceMap = new ConcurrentHashMap<>();
    static {
        objectMapper.registerModule(new JavaTimeModule());
    }

    public static String writeValueAsString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("JSON序列化对象 {} 失败", value);
            throw new JsonParseException(e);
        }
    }

    public static <T> T readValue(String value, Class<T> clazz) {
        TypeReference<T> typeReference = referenceMap.computeIfAbsent(clazz, c -> new TypeReference<T>(){});
        return readValue(value, typeReference);
    }

    public static <T> T readValue(String value, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(value, typeReference);
        } catch (JsonProcessingException e) {
            log.error("JSON反序列化对象 {} 失败", value);
            throw new JsonParseException(e);
        }
    }
}
