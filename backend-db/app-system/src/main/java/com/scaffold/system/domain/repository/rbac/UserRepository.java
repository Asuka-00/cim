package com.scaffold.system.domain.repository.rbac;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.system.domain.entity.rbac.User;
import com.scaffold.system.domain.query.rbac.UserQuery;

/**
 * UserRepository仓储接口
 * <p>
 * 负责定义UserRepository相关数据访问能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
public interface UserRepository {

    /**
     * 根据ID查询用户
     */
    Optional<User> findById(Long id);

    /**
     * 根据ID集合批量查询用户
     */
    List<User> findByIds(Set<Long> ids);

    /**
     * 根据用户账号查询用户
     */
    Optional<User> findByUserName(String userName);

    /**
     * 根据用户账号和租户ID查询用户
     */
    Optional<User> selectByUserNameWithTenant(String userName, String tenantId);

    /**
     * 查询所有用户
     */
    List<User> findAll();

    /**
     * 查询当前有效记录：enabled==ENABLED。
     */
    List<User> findActiveAll();

    List<User> findAllByTenantId(String tenantId);

    /**
     * 按租户统计用户数量（删除引用校验，不加载全量列表）。
     */
    long countByTenantId(String tenantId);

    /**
     * 保存用户
     */
    void save(User user);

    /**
     * 更新用户
     */
    void update(User user);

    /**
     * 根据ID删除用户
     */
    void deleteById(Long id);

    /**
     * 分页查询用户
     *
     * @param page     当前页码
     * @param pageSize 每页大小
     * @param query    查询条件
     * @return 分页结果
     */
    PageDTO<User> queryUserPage(int page, int pageSize, UserQuery query);
}
