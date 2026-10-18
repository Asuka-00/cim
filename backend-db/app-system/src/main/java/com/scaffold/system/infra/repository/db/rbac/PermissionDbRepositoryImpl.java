package com.scaffold.system.infra.repository.db.rbac;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.share.enums.StateEnum;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;
import com.scaffold.system.domain.entity.rbac.Permission;
import com.scaffold.system.domain.entity.rbac.RolePermission;
import com.scaffold.system.domain.entity.rbac.UserRole;
import com.scaffold.system.domain.query.rbac.PermissionQuery;
import com.scaffold.system.domain.repository.rbac.PermissionRepository;
import com.scaffold.system.domain.repository.rbac.RolePermissionRepository;
import com.scaffold.system.domain.repository.rbac.UserRoleRepository;
import com.scaffold.system.infra.mapper.db.rbac.PermissionMapper;

import lombok.RequiredArgsConstructor;

/**
 * PermissionDbRepositoryImpl implements PermissionRepository DB 热库仓储实现。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-05-28
 */
@Repository
@RequiredArgsConstructor
public class PermissionDbRepositoryImpl implements PermissionRepository {

    private final PermissionMapper permissionMapper;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;

    private DbRepositorySupport<Permission> support() {
        return new DbRepositorySupport<>(permissionMapper, Permission.class);
    }

    @Override
    public Optional<Permission> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<Permission> findByPermissionCode(String permissionCode) {
        if (isBlank(permissionCode))
            return Optional.empty();
        return support().findOne(support().wrapper().eq(Permission::getPermissionCode, permissionCode));
    }

    @Override
    public List<Permission> findAll() {
        return support().findAll();
    }

    @Override
    public List<Permission> findActiveAll() {
        return support().queryAll(support().wrapper()
                .eq(Permission::getEnabled, StateEnum.ENABLED));
    }

    @Override
    public List<Permission> findAllByTenantId(String tenantId) {
        if (isBlank(tenantId))
            return List.of();
        return support().queryAll(support().wrapper().eq(Permission::getTenantId, tenantId));
    }

    @Override
    public void save(Permission permission) {
        support().save(permission);
    }

    @Override
    public void update(Permission permission) {
        support().update(permission);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }

    @Override
    public List<Permission> findByParentId(Long parentId) {
        if (parentId == null)
            return List.of();
        return support().queryAll(support().wrapper().eq(Permission::getParentId, parentId));
    }

    @Override
    public List<Permission> findByRoleId(Long roleId) {
        Set<Long> permissionIds = rolePermissionRepository.findByRoleId(roleId).stream()
                .map(RolePermission::getPermissionId).collect(Collectors.toSet());
        return support().findByIds(permissionIds);
    }

    @Override
    public List<Permission> findByUserId(Long userId) {
        Set<Long> roleIds = userRoleRepository.findByUserId(userId).stream().map(UserRole::getRoleId)
                .collect(Collectors.toSet());
        Set<Long> permissionIds = rolePermissionRepository.findByRoleIds(roleIds).stream()
                .map(RolePermission::getPermissionId).collect(Collectors.toSet());
        return support().findByIds(permissionIds);
    }

    @Override
    public PageDTO<Permission> queryPermissionPage(int page, int pageSize, PermissionQuery query) {
        LambdaQueryWrapper<Permission> wrapper = support().wrapper();
        if (query != null) {
            wrapper.eq(hasText(query.getPermissionCode()), Permission::getPermissionCode, query.getPermissionCode())
                    .like(hasText(query.getPermissionName()), Permission::getPermissionName, query.getPermissionName())
                    .eq(query.getType() != null, Permission::getType, query.getType())
                    .eq(query.getParentId() != null, Permission::getParentId, query.getParentId())
                    .eq(query.getVisible() != null, Permission::getVisible, query.getVisible())
                    .eq(query.getEnabled() != null, Permission::getEnabled, query.getEnabled());
        }
        return support().queryPage(page, pageSize, wrapper);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

}
