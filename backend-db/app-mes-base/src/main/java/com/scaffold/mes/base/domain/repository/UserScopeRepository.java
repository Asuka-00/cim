package com.scaffold.mes.base.domain.repository;

import java.util.List;

import com.scaffold.mes.base.domain.entity.UserScope;

/**
 * UserScopeRepository 用户数据权限仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface UserScopeRepository {

    /**
     * 这个用户在该类型下有没有记录。
     *
     * @param userName 用户
     * @param scopeType 类型
     * @return 有记录时为 true
     */
    boolean existsType(String userName, String scopeType);

    /**
     * 这个对象是否在用户范围内。
     *
     * @param userName 用户
     * @param scopeType 类型
     * @param scopeId 对象
     * @return 已授权时为 true
     */
    boolean exists(String userName, String scopeType, Long scopeId);

    /**
     * 列出全部范围，按用户名。
     *
     * @return 范围
     */
    List<UserScope> list();

    /**
     * 插入。
     *
     * @param scope 权限
     */
    void save(UserScope scope);
}
