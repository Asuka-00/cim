package com.scaffold.system.infra.repository.db.rbac;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.scaffold.share.infra.repository.db.DbRepositorySupport;
import com.scaffold.system.domain.entity.rbac.UserRole;
import com.scaffold.system.domain.repository.rbac.UserRoleRepository;
import com.scaffold.system.infra.mapper.db.rbac.UserRoleMapper;

import lombok.RequiredArgsConstructor;

/**
 * UserRoleDbRepositoryImpl implements UserRoleRepository DB 热库仓储实现。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-05-28
 */
@Repository
@RequiredArgsConstructor
public class UserRoleDbRepositoryImpl implements UserRoleRepository {

    private final UserRoleMapper userRoleMapper;

    private DbRepositorySupport<UserRole> support() {
        return new DbRepositorySupport<>(userRoleMapper, UserRole.class);
    }

    @Override
    public Optional<UserRole> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public List<UserRole> findByUserId(Long userId) {
        return findByNumber(UserRole::getUserId, userId);
    }

    @Override
    public List<UserRole> findByRoleId(Long roleId) {
        return findByNumber(UserRole::getRoleId, roleId);
    }

    @Override
    public Optional<UserRole> findByUserIdAndRoleId(Long userId, Long roleId) {
        if (userId == null || roleId == null)
            return Optional.empty();
        return support().findOne(support().wrapper().eq(UserRole::getUserId, userId).eq(UserRole::getRoleId, roleId));
    }

    @Override
    public void save(UserRole userRole) {
        support().save(userRole);
    }

    @Override
    public void saveBatch(List<UserRole> userRoles) {
        support().batchSave(userRoles);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }

    @Override
    public void deleteByUserId(Long userId) {
        deleteByNumber(UserRole::getUserId, userId);
    }

    @Override
    public void deleteByRoleId(Long roleId) {
        deleteByNumber(UserRole::getRoleId, roleId);
    }

    private List<UserRole> findByNumber(com.baomidou.mybatisplus.core.toolkit.support.SFunction<UserRole, Long> column,
            Long value) {
        if (value == null)
            return List.of();
        return support().queryAll(support().wrapper().eq(column, value));
    }

    private void deleteByNumber(com.baomidou.mybatisplus.core.toolkit.support.SFunction<UserRole, Long> column,
            Long value) {
        if (value == null)
            return;
        support().batchDelete(
                support().queryAll(support().wrapper().eq(column, value)).stream().map(UserRole::getId).toList());
    }

}
