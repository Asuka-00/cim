package com.scaffold.system.domain.repository.rbac;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.system.domain.entity.rbac.Role;
import com.scaffold.system.domain.query.rbac.RoleQuery;

/**
 * RoleRepository仓储接口
 * <p>
 * 负责定义RoleRepository相关数据访问能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
public interface RoleRepository {

    /**
     * 根据ID查询角色
     */
    Optional<Role> findById(Long id);

    /**
     * 根据ID列表批量查询角色
     */
    List<Role> findAllById(List<Long> ids);

    /**
     * 根据角色编码查询角色
     */
    Optional<Role> findByRoleCode(String roleCode);

    /**
     * 查询所有角色
     */
    List<Role> findAll();

    /**
     * 查询当前有效记录：enabled==ENABLED。
     */
    List<Role> findActiveAll();

    List<Role> findAllByTenantId(String tenantId);

    /**
     * 保存角色
     */
    void save(Role role);

    /**
     * 更新角色
     */
    void update(Role role);

    /**
     * 根据ID删除角色
     */
    void deleteById(Long id);

    /**
     * 根据用户ID查询角色列表
     */
    List<Role> findByUserId(Long userId);

    /**
     * 分页查询角色
     */
    PageDTO<Role> queryRolePage(int page, int pageSize, RoleQuery query);
}
