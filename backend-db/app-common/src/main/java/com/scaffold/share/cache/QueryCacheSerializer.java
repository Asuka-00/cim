package com.scaffold.share.cache;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * QueryCacheSerializer 查询结果序列化。
 * <p>
 * 使用独立 ObjectMapper 并开启全量类型信息，保证 Long / BigDecimal / 实体等往返后类型不变。
 * 反序列化只放行本项目与 JDK 基础包，避免缓存被污染后触发任意类实例化。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-15
 */
public class QueryCacheSerializer {

    private final ObjectMapper mapper;

    @SuppressWarnings("deprecation")
    public QueryCacheSerializer() {
        PolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.scaffold.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.lang.")
                .allowIfSubType("java.math.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.sql.")
                .build();
        this.mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .serializationInclusion(JsonInclude.Include.ALWAYS)
                .activateDefaultTyping(validator, DefaultTyping.EVERYTHING, JsonTypeInfo.As.PROPERTY)
                .build();
    }

    /**
     * 序列化查询结果
     */
    public String write(List<?> rows) throws Exception {
        return mapper.writeValueAsString(new ArrayList<>(rows));
    }

    /**
     * 反序列化查询结果
     */
    @SuppressWarnings("unchecked")
    public List<Object> read(String json) throws Exception {
        Object value = mapper.readValue(json, Object.class);
        return (List<Object>) value;
    }
}
