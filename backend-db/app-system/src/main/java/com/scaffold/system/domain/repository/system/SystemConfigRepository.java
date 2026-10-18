package com.scaffold.system.domain.repository.system;

import java.util.List;
import java.util.Optional;

import com.scaffold.system.domain.entity.system.SystemConfig;
import com.scaffold.system.domain.query.system.SystemConfigQuery;

/**
 * SystemConfigRepository仓储接口
 * <p>
 * 负责定义SystemConfigRepository相关数据访问能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
public interface SystemConfigRepository {

    /**
     * 根据ID查询系统配置
     */
    Optional<SystemConfig> findById(Long id);

    /**
     * 根据配置编码和租户ID查询系统配置
     */
    Optional<SystemConfig> findByConfigCode(String configCode);

    /**
     * 查询所有系统配置
     */
    List<SystemConfig> findAll();

    List<SystemConfig> findAllByTenantId(String tenantId);

    /**
     * 保存系统配置
     */
    void save(SystemConfig systemConfig);

    /**
     * 更新系统配置
     */
    void update(SystemConfig systemConfig);

    /**
     * 根据ID删除系统配置
     */
    void deleteById(Long id);

    /**
     * 查询系统配置列表（全量）
     *
     * @param query 查询条件
     * @return 配置列表
     */
    List<SystemConfig> querySystemConfigList(SystemConfigQuery query);

    /**
     * 按分组查询全部配置（忽略租户）：登录前实例级配置（如 OAuth2）
     */
    List<SystemConfig> findAllByGroupCodeIgnoreTenant(String groupCode);
}
