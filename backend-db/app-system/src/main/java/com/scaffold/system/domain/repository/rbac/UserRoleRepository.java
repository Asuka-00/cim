package com.scaffold.system.domain.repository.rbac;

import java.util.List;
import java.util.Optional;

import com.scaffold.system.domain.entity.rbac.UserRole;

/**
 * UserRoleRepository仓储接口
 * <p>
 * 负责定义UserRoleRepository相关数据访问能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
public interface UserRoleRepository {

    /**
     * 根据ID查询用户角色关联
     */
    Optional<UserRole> findById(Long id);

    /**
     * 根据用户ID查询用户角色关联列表
     */
    List<UserRole> findByUserId(Long userId);

    /**
     * 根据角色ID查询用户角色关联列表
     */
    List<UserRole> findByRoleId(Long roleId);

    /**
     * 根据用户ID和角色ID查询关联
     */
    Optional<UserRole> findByUserIdAndRoleId(Long userId, Long roleId);

    /**
     * 保存用户角色关联
     */
    void save(UserRole userRole);

    /**
     * 批量保存用户角色关联
     */
    void saveBatch(List<UserRole> userRoles);

    /**
     * 删除用户角色关联
     */
    void deleteById(Long id);

    /**
     * 根据用户ID删除所有角色关联
     */
    void deleteByUserId(Long userId);

    /**
     * 根据角色ID删除所有用户关联
     */
    void deleteByRoleId(Long roleId);
}
