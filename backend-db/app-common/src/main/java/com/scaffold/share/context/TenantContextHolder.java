package com.scaffold.share.context;

import java.util.function.Supplier;

/**
 * TenantContextHolder 租户上下文持有者。
 * <p>
 * 为异步/非Web线程提供当前租户上下文（如定时任务线程），也用于临时切换租户写成员角色。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-05-15
 */
public final class TenantContextHolder {

    private static final ThreadLocal<String> TENANT_ID_HOLDER = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    /**
     * 设置租户标识
     */
    public static void setTenantId(String tenantId) {
        TENANT_ID_HOLDER.set(tenantId);
    }

    /**
     * 获取租户标识
     */
    public static String getTenantId() {
        return TENANT_ID_HOLDER.get();
    }

    /**
     * 清理数据
     */
    public static void clear() {
        TENANT_ID_HOLDER.remove();
    }

    /**
     * 在指定租户上下文中执行，结束后恢复原值。
     */
    public static <T> T runAs(String tenantId, Supplier<T> action) {
        String previous = getTenantId();
        try {
            setTenantId(tenantId);
            return action.get();
        } finally {
            if (previous == null) {
                clear();
            } else {
                setTenantId(previous);
            }
        }
    }

    /**
     * 在指定租户上下文中执行，结束后恢复原值。
     */
    public static void runAs(String tenantId, Runnable action) {
        runAs(tenantId, () -> {
            action.run();
            return null;
        });
    }
}
