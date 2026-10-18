package com.scaffold.system.domain.repository.rbac;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.scaffold.system.domain.entity.rbac.RolePermission;

/**
 * RolePermissionRepository仓储接口
 * <p>
 * 负责定义RolePermissionRepository相关数据访问能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
public interface RolePermissionRepository {

    /**
     * 根据ID查询角色权限关联
     */
    Optional<RolePermission> findById(Long id);

    /**
     * 根据角色ID查询角色权限关联列表
     */
    List<RolePermission> findByRoleId(Long roleId);

    /**
     * 根据角色ID集合查询角色权限关联列表
     */
    List<RolePermission> findByRoleIds(Set<Long> roleIds);

    /**
     * 根据权限ID查询角色权限关联列表
     */
    List<RolePermission> findByPermissionId(Long permissionId);

    /**
     * 根据角色ID和权限ID查询关联
     */
    Optional<RolePermission> findByRoleIdAndPermissionId(Long roleId, Long permissionId);

    /**
     * 保存角色权限关联
     */
    void save(RolePermission rolePermission);

    /**
     * 批量保存角色权限关联
     */
    void saveBatch(List<RolePermission> rolePermissions);

    /**
     * 删除角色权限关联
     */
    void deleteById(Long id);

    /**
     * 批量删除角色权限关联
     */
    void deleteByIds(List<Long> ids);

    /**
     * 根据角色ID删除所有权限关联
     */
    void deleteByRoleId(Long roleId);

    /**
     * 根据权限ID删除所有角色关联
     */
    void deleteByPermissionId(Long permissionId);
}
