package com.scaffold.system.infra.security.rbac;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.context.SessionPrincipal;
import com.scaffold.share.context.TenantContextHolder;
import com.scaffold.share.context.UserContextHolder;

/**
 * SecurityCurrentSessionProvider类
 * <p>
 * 负责处理SecurityCurrentSessionProvider相关核心业务能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Component
public class SecurityCurrentSessionProvider implements CurrentSessionProvider {

    @Override
    public String getCurrentUserName() {
        Object principal = getPrincipal();
        if (principal instanceof SessionPrincipal sessionPrincipal) {
            return sessionPrincipal.getUserName();
        }
        if (principal instanceof String userName) {
            return userName;
        }
        String userNameFromContext = UserContextHolder.getUserName();
        if (userNameFromContext != null && !userNameFromContext.isBlank()) {
            return userNameFromContext;
        }
        return null;
    }

    /**
     * 取得当前租户。
     * <p>
     * {@code runAs} 写入的显式租户优先于登录主体,否则请求内切换租户不生效。
     *
     * @return 租户标识,两者都没有时为 null
     */
    @Override
    public String getCurrentTenantId() {
        String tenantIdFromContext = TenantContextHolder.getTenantId();
        if (tenantIdFromContext != null && !tenantIdFromContext.isBlank()) {
            return tenantIdFromContext;
        }
        Object principal = getPrincipal();
        if (principal instanceof SessionPrincipal sessionPrincipal) {
            return sessionPrincipal.getTenantId();
        }
        return null;
    }

    /**
     * 获取认证主体
     */
    private Object getPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        return authentication.getPrincipal();
    }
}
