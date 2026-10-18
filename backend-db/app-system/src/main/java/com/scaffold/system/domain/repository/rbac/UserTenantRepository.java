package com.scaffold.system.domain.repository.rbac;

import java.util.List;
import java.util.Optional;

import com.scaffold.system.domain.entity.rbac.UserTenant;

/**
 * UserTenantRepository 用户-租户成员资格仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-09-11
 */
public interface UserTenantRepository {

    List<UserTenant> findByUserId(Long userId);

    List<UserTenant> findByTenantId(String tenantId);

    List<Long> findUserIdsByTenantId(String tenantId);

    Optional<UserTenant> findByUserIdAndTenantId(Long userId, String tenantId);

    boolean existsByUserIdAndTenantId(Long userId, String tenantId);

    long countByTenantId(String tenantId);

    void save(UserTenant userTenant);

    void saveBatch(List<UserTenant> memberships);

    void deleteByUserId(Long userId);
}
