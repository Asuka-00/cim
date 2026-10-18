package com.scaffold.share.logging;

import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

import com.scaffold.share.context.CurrentSessionProvider;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 在 Security 之后补全 MDC 中的用户与租户信息。
 * <p>
 * traceId 的生成与清理由 {@link TraceIdFilter} 负责，本拦截器不再 clear MDC。
 *
 * @author Kairo
 * @version 1.1.0
 * @since 2026-04-09
 */
public class TraceIdInterceptor implements HandlerInterceptor {

    private static final String USER_CODE_HEADER = "X-User-Code";
    private static final String TENANT_ID_HEADER = "X-Tenant-Id";

    private final CurrentSessionProvider currentSessionProvider;

    public TraceIdInterceptor(CurrentSessionProvider currentSessionProvider) {
        this.currentSessionProvider = currentSessionProvider;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull Object handler) {
        // 极端路径下 Filter 未生效时兜底
        MdcUtils.getOrCreateTraceId();

        String userName = resolveUserName(request);
        if (userName != null && !userName.isBlank()) {
            MdcUtils.setUserName(userName);
        }

        MdcUtils.setTenantId(resolveTenantId(request));
        return true;
    }

    private String resolveUserName(HttpServletRequest request) {
        String userName = currentSessionProvider.getCurrentUserName();
        if (userName != null && !userName.isBlank()) {
            return userName;
        }
        return request.getHeader(USER_CODE_HEADER);
    }

    private String resolveTenantId(HttpServletRequest request) {
        String tenantId = currentSessionProvider.getCurrentTenantId();
        if (tenantId != null && !tenantId.isBlank()) {
            return tenantId;
        }
        tenantId = request.getHeader(TENANT_ID_HEADER);
        if (tenantId != null && !tenantId.isBlank()) {
            return tenantId;
        }
        return CurrentSessionProvider.DEFAULT_TENANT_ID;
    }
}
