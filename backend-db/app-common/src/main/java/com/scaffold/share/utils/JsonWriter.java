package com.scaffold.share.utils;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * JsonWriter 通用 JSON 序列化助手。
 * <p>
 * 将对象序列化为 JSON 字符串以写入 JSON 列（如 breakdown / whyNot / audit 汇总等）。
 * 序列化失败<b>响亮失败</b>（抛出 {@link IllegalStateException}），
 * 让证据/审计列序列化问题被上层 persist 捕获并记为 FAILED 运行行，而非静默写出空证据却标 SUCCEEDED。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-25
 */
@Component
@RequiredArgsConstructor
public class JsonWriter {

    private final ObjectMapper objectMapper;

    /**
     * 序列化对象为 JSON 字符串；null 入参返回 null，序列化异常向上抛出。
     *
     * @param value 待序列化对象
     * @return JSON 字符串（入参 null 时返回 null）
     * @throws IllegalStateException 序列化失败
     */
    public String write(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to serialize json: type=" + value.getClass().getSimpleName(), ex);
        }
    }
}
