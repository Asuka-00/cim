package com.scaffold.mes.exec.domain.service;

/**
 * ReleaseRoleChecker 当前操作人是否拥有指定角色
 * <p>
 * 登录态里还没有角色列表。默认实现一律回答否，因此带角色要求的 Release 会被拒绝。角色为空时服务不调用本接口。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface ReleaseRoleChecker {

    /**
     * 当前操作人是否拥有该角色。
     *
     * @param roleCode 角色编码
     * @return 拥有时为 true
     */
    boolean has(String roleCode);
}
