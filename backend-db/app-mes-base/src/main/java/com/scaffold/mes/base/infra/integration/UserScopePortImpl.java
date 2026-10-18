package com.scaffold.mes.base.infra.integration;

import org.springframework.stereotype.Component;

import com.scaffold.mes.base.domain.service.UserScopeApplicationService;
import com.scaffold.share.integration.mes.base.provider.UserScopePort;

import lombok.RequiredArgsConstructor;

/**
 * UserScopePortImpl 用户数据权限
 * <p>
 * 转给范围服务判断。没有记录时允许。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
public class UserScopePortImpl implements UserScopePort {

    private final UserScopeApplicationService scopes;

    @Override
    public boolean allowed(String userName, String scopeType, Long scopeId) {
        return scopes.allowed(userName, scopeType, scopeId);
    }
}
