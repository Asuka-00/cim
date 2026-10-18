package com.scaffold.share.web;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.share.vo.Result;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * ApiErrorResponseWriter响应写出组件
 * <p>
 * 负责统一输出错误响应 JSON，避免各层重复拼接返回体
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-21
 */
@Component
@RequiredArgsConstructor
public class ApiErrorResponseWriter {

    private final ObjectMapper objectMapper;

    /**
     * 写出统一错误响应
     */
    public void write(HttpServletResponse response, int status, int code, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Result.fail(code, message));
    }
}