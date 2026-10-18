package com.scaffold.share.logging;

import java.io.IOException;
import java.util.UUID;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * HTTP 入口尽早建立 traceId / 请求元数据 MDC，并在请求结束统一清理。
 * <p>
 * 早于业务 Filter，使 Security / ParameterTrimming 等日志也能关联 trace。
 * 用户与租户信息由 {@link TraceIdInterceptor} 在 Security 之后补全。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-07-29
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-ID";

    private static final String REQUEST_IP_HEADER = "X-Real-IP";
    private static final String USER_AGENT_HEADER = "User-Agent";

    private static final String[] EXCLUDE_PREFIXES = {
            "/swagger-ui",
            "/v3/api-docs",
            "/favicon.ico"
    };

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return false;
        }
        for (String prefix : EXCLUDE_PREFIXES) {
            if (path.startsWith(prefix) || path.contains(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        MdcUtils.setTraceId(traceId);
        MdcUtils.setRequestUri(request.getRequestURI());
        MdcUtils.setRequestMethod(request.getMethod());
        MdcUtils.setRequestIp(resolveClientIp(request));
        String userAgent = request.getHeader(USER_AGENT_HEADER);
        if (userAgent != null) {
            MdcUtils.put(LogConstants.USER_AGENT, userAgent);
        }
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MdcUtils.clear();
        }
    }

    static String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader(REQUEST_IP_HEADER);
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
