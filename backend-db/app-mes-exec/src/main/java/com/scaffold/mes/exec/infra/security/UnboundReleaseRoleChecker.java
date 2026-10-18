package com.scaffold.mes.exec.infra.security;

import org.springframework.stereotype.Component;

import com.scaffold.mes.exec.domain.service.ReleaseRoleChecker;

/**
 * UnboundReleaseRoleChecker 尚未接入登录角色
 * <p>
 * 当前会话只有用户名。无法证明操作人拥有要求的角色时，回答否。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
public class UnboundReleaseRoleChecker implements ReleaseRoleChecker {

    @Override
    public boolean has(String roleCode) {
        return false;
    }
}
