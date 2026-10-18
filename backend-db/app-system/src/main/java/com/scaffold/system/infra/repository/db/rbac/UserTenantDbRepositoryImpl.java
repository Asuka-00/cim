package com.scaffold.system.infra.repository.db.rbac;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.scaffold.share.infra.repository.db.DbRepositorySupport;
import com.scaffold.system.domain.entity.rbac.UserTenant;
import com.scaffold.system.domain.repository.rbac.UserTenantRepository;
import com.scaffold.system.infra.mapper.db.rbac.UserTenantMapper;

import lombok.RequiredArgsConstructor;

/**
 * UserTenantDbRepositoryImpl DB 热库仓储。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-09-11
 */
@Repository
@RequiredArgsConstructor
public class UserTenantDbRepositoryImpl implements UserTenantRepository {

    private final UserTenantMapper userTenantMapper;

    private DbRepositorySupport<UserTenant> support() {
        return new DbRepositorySupport<>(userTenantMapper, UserTenant.class);
    }

    @Override
    public List<UserTenant> findByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return support().queryAll(support().wrapper().eq(UserTenant::getUserId, userId));
    }

    @Override
    public List<UserTenant> findByTenantId(String tenantId) {
        if (support().isBlank(tenantId)) {
            return List.of();
        }
        return support().queryAll(support().wrapper().eq(UserTenant::getTenantId, tenantId));
    }

    @Override
    public List<Long> findUserIdsByTenantId(String tenantId) {
        return findByTenantId(tenantId).stream().map(UserTenant::getUserId).toList();
    }

    @Override
    public Optional<UserTenant> findByUserIdAndTenantId(Long userId, String tenantId) {
        if (userId == null || support().isBlank(tenantId)) {
            return Optional.empty();
        }
        return support().findOne(support().wrapper()
                .eq(UserTenant::getUserId, userId)
                .eq(UserTenant::getTenantId, tenantId));
    }

    @Override
    public boolean existsByUserIdAndTenantId(Long userId, String tenantId) {
        return findByUserIdAndTenantId(userId, tenantId).isPresent();
    }

    @Override
    public long countByTenantId(String tenantId) {
        if (support().isBlank(tenantId)) {
            return 0L;
        }
        return findByTenantId(tenantId).size();
    }

    @Override
    public void save(UserTenant userTenant) {
        support().save(userTenant);
    }

    @Override
    public void saveBatch(List<UserTenant> memberships) {
        support().batchSave(memberships);
    }

    @Override
    public void deleteByUserId(Long userId) {
        if (userId == null) {
            return;
        }
        support().batchDelete(findByUserId(userId).stream().map(UserTenant::getId).toList());
    }
}
