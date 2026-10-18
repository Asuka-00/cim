package com.scaffold.share.context;

/**
 * 当前会话主体。
 * <p>
 * 用于在认证上下文中携带跨模块通用的用户与租户信息。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-21
 */
public final class SessionPrincipal {

    private final String userName;
    private final String tenantId;

    public SessionPrincipal(String userName, String tenantId) {
        this.userName = userName;
        this.tenantId = tenantId;
    }

    /**
     * 获取用户编码
     */
    public String getUserName() {
        return userName;
    }

    /**
     * 获取租户标识
     */
    public String getTenantId() {
        return tenantId;
    }
}
