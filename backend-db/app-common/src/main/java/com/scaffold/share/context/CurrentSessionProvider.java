package com.scaffold.share.context;

/**
 * CurrentSessionProvider接口
 * <p>
 * 负责定义CurrentSessionProvider相关行为契约
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
public interface CurrentSessionProvider {

    String SYSTEM_USER = "SYSTEM";
    String DEFAULT_TENANT_ID = "DEFAULT_TENANT";

    /**
     * 获取当前用户编码
     */
    String getCurrentUserName();

    /**
     * 获取当前租户标识
     */
    String getCurrentTenantId();

    /**
     * 获取当前用户编码，为空时返回默认值
     */
    default String getCurrentUserNameOrDefault() {
        String userName = getCurrentUserName();
        if (userName == null || userName.isBlank()) {
            return SYSTEM_USER;
        }
        return userName;
    }

    /**
     * 获取当前租户标识，为空时返回默认值
     */
    default String getCurrentTenantIdOrDefault() {
        String tenantId = getCurrentTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return DEFAULT_TENANT_ID;
        }
        return tenantId;
    }
}
