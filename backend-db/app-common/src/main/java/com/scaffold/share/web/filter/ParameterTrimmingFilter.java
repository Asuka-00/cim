package com.scaffold.share.web.filter;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * ParameterTrimmingFilter过滤器类
 * <p>
 * 负责处理ParameterTrimmingFilter相关请求过滤与预处理逻辑
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ParameterTrimmingFilter extends OncePerRequestFilter {

    private static final String[] EXCLUDE_PATHS = {
            "/swagger-ui",
            "/v3/api-docs",
            "/favicon.ico"
    };

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        filterChain.doFilter(new TrimmingHttpServletRequest(request), response);
    }

    /**
     * 判断是否跳过过滤
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return Arrays.stream(EXCLUDE_PATHS).anyMatch(path::startsWith);
    }

    private static class TrimmingHttpServletRequest extends HttpServletRequestWrapper {
        public TrimmingHttpServletRequest(HttpServletRequest request) {
            super(request);
        }

        /**
         * 获取请求参数
         */
        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return trimToNull(value);
        }

        /**
         * 获取参数值
         */
        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null)
                return null;
            return Arrays.stream(values)
                    .map(TrimmingHttpServletRequest::trimToNull)
                    .toArray(String[]::new);
        }

        /**
         * 将空白字符串转换为 null
         */
        private static String trimToNull(String value) {
            if (value == null)
                return null;
            String trimmed = value.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
    }
}
