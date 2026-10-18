package com.scaffold.share.logging;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * AuditPayloadSerializer序列化器
 * <p>
 * 负责将审计入参和出参转换为安全、受限长度的JSON字符串。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-23
 */
@RequiredArgsConstructor
public class AuditPayloadSerializer {

    private static final int MAX_DEPTH = 3;
    private static final String SERIALIZATION_FAILED = "{\"_error\":\"serialization failed\"}";

    private final ObjectMapper objectMapper;

    /**
     * 序列化方法参数
     *
     * @param paramNames       参数名数组
     * @param args             参数值数组
     * @param mask             是否脱敏
     * @param maxPayloadLength 最大payload长度
     * @return JSON字符串
     */
    public String serializeArguments(String[] paramNames, Object[] args, boolean mask, int maxPayloadLength) {
        if (args == null || args.length == 0) {
            return "{}";
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (shouldSkip(arg)) {
                continue;
            }

            String paramName = resolveParamName(paramNames, i);
            payload.put(paramName, sanitizeValue(paramName, arg, mask, 0));
        }

        return writeLimited(payload, maxPayloadLength);
    }

    /**
     * 序列化单个返回值
     *
     * @param value            返回值
     * @param mask             是否脱敏
     * @param maxPayloadLength 最大payload长度
     * @return JSON字符串
     */
    public String serializeValue(Object value, boolean mask, int maxPayloadLength) {
        if (value == null) {
            return "null";
        }
        if (shouldSkip(value)) {
            return "\"<skipped>\"";
        }
        return writeLimited(sanitizeValue("response", value, mask, 0), maxPayloadLength);
    }

    private String resolveParamName(String[] paramNames, int index) {
        if (paramNames == null || index >= paramNames.length || paramNames[index] == null
                || paramNames[index].isBlank()) {
            return "arg" + index;
        }
        return paramNames[index];
    }

    private String writeLimited(Object value, int maxPayloadLength) {
        try {
            String json = objectMapper.writeValueAsString(value);
            return limit(json, maxPayloadLength);
        } catch (JsonProcessingException | RuntimeException ex) {
            return SERIALIZATION_FAILED;
        }
    }

    private String limit(String json, int maxPayloadLength) {
        if (json == null || maxPayloadLength <= 0 || json.length() <= maxPayloadLength) {
            return json;
        }
        return json.substring(0, maxPayloadLength) + "...<truncated>";
    }

    private Object sanitizeValue(String fieldName, Object value, boolean mask, int depth) {
        if (value == null) {
            return null;
        }
        if (shouldSkip(value)) {
            return "<skipped>";
        }
        if (depth > MAX_DEPTH) {
            return value.getClass().getSimpleName() + "@" + System.identityHashCode(value);
        }
        if (isSimpleType(value)) {
            if (mask && DataMaskingUtils.isSensitiveField(fieldName)) {
                return DataMaskingUtils.maskByFieldName(fieldName, value);
            }
            return value;
        }
        if (value instanceof Collection<?> collection) {
            Map<String, Object> collectionInfo = new LinkedHashMap<>();
            collectionInfo.put("_type", "Collection");
            collectionInfo.put("_size", collection.size());
            if (!collection.isEmpty()) {
                collectionInfo.put("_sample", sanitizeValue(fieldName, collection.iterator().next(), mask, depth + 1));
            }
            return collectionInfo;
        }
        if (value.getClass().isArray()) {
            Map<String, Object> arrayInfo = new LinkedHashMap<>();
            arrayInfo.put("_type", "Array");
            arrayInfo.put("_length", Array.getLength(value));
            arrayInfo.put("_componentType", value.getClass().getComponentType().getSimpleName());
            return arrayInfo;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> mapResult = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                mapResult.put(key, sanitizeValue(key, entry.getValue(), mask, depth + 1));
            }
            return mapResult;
        }
        return extractFields(value, mask, depth);
    }

    private Map<String, Object> extractFields(Object value, boolean mask, int depth) {
        Map<String, Object> fields = new LinkedHashMap<>();
        Class<?> clazz = value.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || "serialVersionUID".equals(field.getName())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    fields.put(field.getName(), sanitizeValue(field.getName(), field.get(value), mask, depth + 1));
                } catch (RuntimeException | IllegalAccessException ex) {
                    fields.put(field.getName(), "<access denied>");
                }
            }
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    private boolean shouldSkip(Object value) {
        return value instanceof ServletRequest
                || value instanceof ServletResponse
                || value instanceof MultipartFile
                || value instanceof BindingResult
                || value instanceof InputStream
                || value instanceof OutputStream
                || value instanceof Reader
                || value instanceof Writer
                || value instanceof Principal;
    }

    private boolean isSimpleType(Object value) {
        return value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof Enum<?>
                || value instanceof Date
                || value instanceof LocalDate
                || value instanceof LocalDateTime
                || value instanceof LocalTime
                || value instanceof Instant
                || value instanceof OffsetDateTime
                || value.getClass().isPrimitive()
                || value.getClass().getName().startsWith("java.lang");
    }
}
