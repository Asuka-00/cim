package com.scaffold.share.context;

import com.scaffold.share.constants.ErrorCodeConstants;
import com.scaffold.share.exception.BusinessException;

/**
 * 当前会话上下文工具类。
 * <p>
 * 基于 {@link CurrentSessionProvider} 读取跨模块通用的当前用户与租户信息。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-21
 */
public final class SessionContextUtils {

    private SessionContextUtils() {
    }

    /**
     * 获取当前用户编码
     */
    public static String getCurrentUserName(CurrentSessionProvider currentSessionProvider) {
        String userName = resolveCurrentUserName(currentSessionProvider);
        if (userName == null || userName.isBlank()) {
            throw new BusinessException(ErrorCodeConstants.SESSION_NOT_FOUND, "未登录或会话不存在");
        }
        return userName;
    }

    /**
     * 获取当前租户标识
     */
    public static String getCurrentTenantId(CurrentSessionProvider currentSessionProvider) {
        String tenantId = resolveCurrentTenantId(currentSessionProvider);
        if (tenantId == null || tenantId.isBlank()) {
            throw new BusinessException(ErrorCodeConstants.TENANT_NOT_FOUND, "无法获取当前租户信息");
        }
        return tenantId;
    }

    /**
     * 获取当前用户编码，为空时返回默认值
     */
    public static String getCurrentUserNameOrDefault(CurrentSessionProvider currentSessionProvider) {
        String userName = resolveCurrentUserName(currentSessionProvider);
        if (userName == null || userName.isBlank()) {
            return CurrentSessionProvider.SYSTEM_USER;
        }
        return userName;
    }

    /**
     * 获取当前租户标识，为空时返回默认值
     */
    public static String getCurrentTenantIdOrDefault(CurrentSessionProvider currentSessionProvider) {
        String tenantId = resolveCurrentTenantId(currentSessionProvider);
        if (tenantId == null || tenantId.isBlank()) {
            return CurrentSessionProvider.DEFAULT_TENANT_ID;
        }
        return tenantId;
    }

    /**
     * 解析当前用户编码
     */
    private static String resolveCurrentUserName(CurrentSessionProvider currentSessionProvider) {
        if (currentSessionProvider != null) {
            String userName = currentSessionProvider.getCurrentUserName();
            if (userName != null && !userName.isBlank()) {
                return userName;
            }
        }
        return UserContextHolder.getUserName();
    }

    /**
     * 解析当前租户标识
     */
    private static String resolveCurrentTenantId(CurrentSessionProvider currentSessionProvider) {
        if (currentSessionProvider != null) {
            String tenantId = currentSessionProvider.getCurrentTenantId();
            if (tenantId != null && !tenantId.isBlank()) {
                return tenantId;
            }
        }
        return TenantContextHolder.getTenantId();
    }
}
