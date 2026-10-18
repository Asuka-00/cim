package com.scaffold.system.infra.repository.db.rbac;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Repository;

import com.scaffold.share.infra.repository.db.DbRepositorySupport;
import com.scaffold.system.domain.entity.rbac.RolePermission;
import com.scaffold.system.domain.repository.rbac.RolePermissionRepository;
import com.scaffold.system.infra.mapper.db.rbac.RolePermissionMapper;

import lombok.RequiredArgsConstructor;

/**
 * RolePermissionDbRepositoryImpl implements RolePermissionRepository DB 热库仓储实现。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-05-28
 */
@Repository
@RequiredArgsConstructor
public class RolePermissionDbRepositoryImpl implements RolePermissionRepository {

    private final RolePermissionMapper rolePermissionMapper;

    private DbRepositorySupport<RolePermission> support() {
        return new DbRepositorySupport<>(rolePermissionMapper, RolePermission.class);
    }

    @Override
    public Optional<RolePermission> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public List<RolePermission> findByRoleId(Long roleId) {
        return findByNumber(RolePermission::getRoleId, roleId);
    }

    @Override
    public List<RolePermission> findByRoleIds(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty())
            return List.of();
        return support().queryAll(support().wrapper().in(RolePermission::getRoleId, roleIds));
    }

    @Override
    public List<RolePermission> findByPermissionId(Long permissionId) {
        return findByNumber(RolePermission::getPermissionId, permissionId);
    }

    @Override
    public Optional<RolePermission> findByRoleIdAndPermissionId(Long roleId, Long permissionId) {
        if (roleId == null || permissionId == null)
            return Optional.empty();
        return support().findOne(support().wrapper().eq(RolePermission::getRoleId, roleId)
                .eq(RolePermission::getPermissionId, permissionId));
    }

    @Override
    public void save(RolePermission rolePermission) {
        support().save(rolePermission);
    }

    @Override
    public void saveBatch(List<RolePermission> rolePermissions) {
        support().batchSave(rolePermissions);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        support().batchDelete(ids);
    }

    @Override
    public void deleteByRoleId(Long roleId) {
        deleteByNumber(RolePermission::getRoleId, roleId);
    }

    @Override
    public void deleteByPermissionId(Long permissionId) {
        deleteByNumber(RolePermission::getPermissionId, permissionId);
    }

    private List<RolePermission> findByNumber(
            com.baomidou.mybatisplus.core.toolkit.support.SFunction<RolePermission, Long> column, Long value) {
        if (value == null)
            return List.of();
        return support().queryAll(support().wrapper().eq(column, value));
    }

    private void deleteByNumber(com.baomidou.mybatisplus.core.toolkit.support.SFunction<RolePermission, Long> column,
            Long value) {
        if (value == null)
            return;
        support().batchDelete(
                support().queryAll(support().wrapper().eq(column, value)).stream().map(RolePermission::getId).toList());
    }

}
