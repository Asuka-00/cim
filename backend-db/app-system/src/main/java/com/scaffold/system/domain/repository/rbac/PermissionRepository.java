package com.scaffold.system.domain.repository.rbac;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.system.domain.entity.rbac.Permission;
import com.scaffold.system.domain.query.rbac.PermissionQuery;

/**
 * PermissionRepository仓储接口
 * <p>
 * 负责定义PermissionRepository相关数据访问能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
public interface PermissionRepository {

    /**
     * 根据ID查询权限
     */
    Optional<Permission> findById(Long id);

    /**
     * 根据权限编码查询权限
     */
    Optional<Permission> findByPermissionCode(String permissionCode);

    /**
     * 查询所有权限
     */
    List<Permission> findAll();

    /**
     * 查询当前有效记录：enabled==ENABLED。
     */
    List<Permission> findActiveAll();

    List<Permission> findAllByTenantId(String tenantId);

    /**
     * 保存权限
     */
    void save(Permission permission);

    /**
     * 更新权限
     */
    void update(Permission permission);

    /**
     * 根据ID删除权限
     */
    void deleteById(Long id);

    /**
     * 根据父ID查询子权限列表
     */
    List<Permission> findByParentId(Long parentId);

    /**
     * 根据角色ID查询权限列表
     */
    List<Permission> findByRoleId(Long roleId);

    /**
     * 根据用户ID查询权限列表
     */
    List<Permission> findByUserId(Long userId);

    /**
     * 分页查询权限
     */
    PageDTO<Permission> queryPermissionPage(int page, int pageSize, PermissionQuery query);
}
