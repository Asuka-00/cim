package com.scaffold.share.integration.system.provider;

/**
 * MesTenantSeedProvider 租户 MES 系统种子
 * <p>
 * 由 app-system 实现。写入字典、运行参数和角色授权,不由 MES 模块直接改 sys_* 表。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface MesTenantSeedProvider {

    /**
     * 为指定租户幂等补齐 MES 字典、运行参数,并把 MES 菜单权限授给该租户角色。
     *
     * @param tenantId 租户标识
     */
    void seed(String tenantId);
}
