package com.scaffold.share.utils;

import com.scaffold.share.constants.RedisKeyConstants;
import com.scaffold.share.context.CurrentSessionProvider;

import lombok.experimental.UtilityClass;

/**
 * RedisKeyBuilder类
 * <p>
 * 提供统一的Redis Key构建方法，消除散落的key格式定义
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-14
 */
@UtilityClass
public class RedisKeyBuilder {

    /**
     * 构建实体Key（带租户隔离）
     * 格式: app:{tenantId}:entityCategory:id
     *
     * @param tenantId       租户ID
     * @param entityCategory 实体类别（如 user, role, permission 等）
     * @param id             实体ID
     * @return 完整的Redis Key
     */
    public static String buildEntityKey(String tenantId, String entityCategory, Long id) {
        // 租户段固定包裹在 {} 中，便于按模式扫描时稳定提取租户信息
        return RedisKeyConstants.NAMESPACE + RedisKeyConstants.SEPARATOR
                + RedisKeyConstants.TENANT_WRAPPER_LEFT + normalizeTenantId(tenantId)
                + RedisKeyConstants.TENANT_WRAPPER_RIGHT
                + RedisKeyConstants.SEPARATOR + entityCategory
                + RedisKeyConstants.SEPARATOR + id;
    }

    /**
     * 构建Auth Key（不带租户，用户维度）
     * 格式: auth:authCategory:identifier
     *
     * @param authCategory 认证子类别（如 userAccess, userRefresh, tokenBlacklist）
     * @param identifier   标识符（如 userName, tokenId）
     * @return 完整的Redis Key
     */
    public static String buildAuthKey(String authCategory, String identifier) {
        return RedisKeyConstants.AUTH_NAMESPACE + RedisKeyConstants.SEPARATOR
                + authCategory + RedisKeyConstants.SEPARATOR + identifier;
    }

    /**
     * 构建Token黑名单Key
     * 格式: auth:tokenBlacklist:tokenId
     *
     * @param tokenId Token ID
     * @return 完整的Redis Key
     */
    public static String buildBlacklistKey(String tokenId) {
        return buildAuthKey(RedisKeyConstants.AUTH_TOKEN_BLACKLIST, tokenId);
    }

    /**
     * 构建实体Key的匹配模式（用于SCAN/KEYS查找单个实体）
     * 格式: app:*:entityCategory:id
     *
     * @param entityCategory 实体类别
     * @param id             实体ID
     * @return 用于匹配的Key模式
     */
    public static String buildEntityKeyPattern(String entityCategory, Long id) {
        return RedisKeyConstants.NAMESPACE + RedisKeyConstants.SEPARATOR
                + RedisKeyConstants.TENANT_WRAPPER_LEFT + "*" + RedisKeyConstants.TENANT_WRAPPER_RIGHT
                + RedisKeyConstants.SEPARATOR + entityCategory
                + RedisKeyConstants.SEPARATOR + id;
    }

    /**
     * 构建实体Key的全量匹配模式（用于SCAN/KEYS查找所有该类型实体）
     * 格式: app:*:entityCategory:*
     *
     * @param entityCategory 实体类别
     * @return 用于匹配的Key模式
     */
    public static String buildEntityAllPattern(String entityCategory) {
        return RedisKeyConstants.NAMESPACE + RedisKeyConstants.SEPARATOR
                + RedisKeyConstants.TENANT_WRAPPER_LEFT + "*" + RedisKeyConstants.TENANT_WRAPPER_RIGHT
                + RedisKeyConstants.SEPARATOR + entityCategory
                + RedisKeyConstants.SEPARATOR + "*";
    }

    /**
     * 构建实体Key的租户内全量匹配模式（用于查找当前租户下该类型实体）
     * 格式: app:{tenantId}:entityCategory:*
     *
     * @param tenantId       租户ID
     * @param entityCategory 实体类别
     * @return 用于匹配的Key模式
     */
    public static String buildEntityTenantAllPattern(String tenantId, String entityCategory) {
        return RedisKeyConstants.NAMESPACE + RedisKeyConstants.SEPARATOR
                + RedisKeyConstants.TENANT_WRAPPER_LEFT + normalizeTenantId(tenantId)
                + RedisKeyConstants.TENANT_WRAPPER_RIGHT
                + RedisKeyConstants.SEPARATOR + entityCategory
                + RedisKeyConstants.SEPARATOR + "*";
    }

    /**
     * 构建租户内二级索引Key
     * 格式: app:{tenantId}:idx:entityCategory:fieldName:fieldValue
     */
    public static String buildEntityIndexKey(String tenantId, String entityCategory, String fieldName,
            String fieldValue) {
        // 二级索引与实体主键同样带租户前缀，防止跨租户命中同一字段值
        return RedisKeyConstants.NAMESPACE + RedisKeyConstants.SEPARATOR
                + RedisKeyConstants.TENANT_WRAPPER_LEFT + normalizeTenantId(tenantId)
                + RedisKeyConstants.TENANT_WRAPPER_RIGHT
                + RedisKeyConstants.SEPARATOR + RedisKeyConstants.INDEX_CATEGORY
                + RedisKeyConstants.SEPARATOR + entityCategory
                + RedisKeyConstants.SEPARATOR + fieldName
                + RedisKeyConstants.SEPARATOR + fieldValue;
    }

    /**
     * 构建实体索引Pattern
     * 格式: app:{tenantId}:idx:entityCategory:*
     */
    public static String buildEntityIndexPattern(String tenantId, String entityCategory) {
        return RedisKeyConstants.NAMESPACE + RedisKeyConstants.SEPARATOR
                + RedisKeyConstants.TENANT_WRAPPER_LEFT + normalizeTenantId(tenantId)
                + RedisKeyConstants.TENANT_WRAPPER_RIGHT
                + RedisKeyConstants.SEPARATOR + RedisKeyConstants.INDEX_CATEGORY
                + RedisKeyConstants.SEPARATOR + entityCategory
                + RedisKeyConstants.SEPARATOR + "*";
    }

    /**
     * 标准化租户ID，空值时返回默认值
     */
    private static String normalizeTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            // 所有未显式传租户的 key 都回落到默认租户，避免生成裸前缀脏 key
            return CurrentSessionProvider.DEFAULT_TENANT_ID;
        }
        return tenantId;
    }
}
