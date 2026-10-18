package com.scaffold.share.constants;

/**
 * RedisKeyConstants类
 * <p>
 * 统一管理所有Redis Key的命名空间、类别前缀和分隔符常量
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-14
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    // ==================== 顶层命名空间 ====================

    /** 实体数据命名空间 */
    public static final String NAMESPACE = "app";

    /** 认证会话命名空间（全局，不区分租户） */
    public static final String AUTH_NAMESPACE = "auth";

    // ==================== 实体类别 (Entity Category) ====================

    /** 用户实体 */
    public static final String ENTITY_USER = "user";

    /** 租户实体 */
    public static final String ENTITY_TENANT = "tenant";

    /** 角色实体 */
    public static final String ENTITY_ROLE = "role";

    /** 权限实体 */
    public static final String ENTITY_PERMISSION = "permission";

    /** 用户-角色关联 */
    public static final String ENTITY_USER_ROLE = "userRole";

    /** 角色-权限关联 */
    public static final String ENTITY_ROLE_PERMISSION = "rolePermission";

    /** 系统配置实体 */
    public static final String ENTITY_SYSTEM_CONFIG = "systemConfig";

    /** 二级索引类别前缀 */
    public static final String INDEX_CATEGORY = "idx";

    // ==================== Auth子类别 (Auth Sub-category) ====================

    /** 用户访问令牌 */
    public static final String AUTH_USER_ACCESS = "userAccess";

    /** 用户刷新令牌 */
    public static final String AUTH_USER_REFRESH = "userRefresh";

    /** Token黑名单 */
    public static final String AUTH_TOKEN_BLACKLIST = "tokenBlacklist";

    /** OAuth2 登录一次性换票凭证 */
    public static final String AUTH_OAUTH2_TICKET = "oauth2Ticket";

    // ==================== Key分隔符 ====================

    /** Key各部分之间的分隔符 */
    public static final char SEPARATOR = ':';

    /** 租户ID包裹符左半边 */
    public static final char TENANT_WRAPPER_LEFT = '{';

    /** 租户ID包裹符右半边 */
    public static final char TENANT_WRAPPER_RIGHT = '}';

    // ==================== Key模板说明 ====================
    // 实体 Key: app:{tenantId}:entityCategory:id
    // Auth Key: auth:authCategory:identifier (不含租户，全局会话)
}
