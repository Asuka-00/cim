package com.scaffold.mes.base.domain.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.UserScopeCreateRequest;
import com.scaffold.mes.base.domain.entity.UserScope;
import com.scaffold.mes.base.domain.repository.UserScopeRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * UserScopeApplicationService 用户数据权限
 * <p>
 * 某个类型没有记录时不限制。有记录时只允许名单里的对象。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class UserScopeApplicationService {

    private static final Set<String> TYPES = Set.of("AREA", "STEP", "EQUIPMENT_GROUP");

    private final UserScopeRepository repository;

    /**
     * 列出全部范围。
     *
     * @return 按用户名排列
     */
    public List<UserScope> list() {
        return repository.list();
    }

    /**
     * 授予一项范围。
     *
     * @param request 用户、类型和对象
     * @throws BusinessException 类型不合法，或同一范围已有记录
     */
    @Transactional
    public void create(UserScopeCreateRequest request) {
        if (!TYPES.contains(request.getScopeType())) {
            throw new BusinessException(MesBaseErrorCodes.USER_SCOPE_INVALID, "数据权限类型不合法");
        }
        if (repository.exists(request.getUserName(), request.getScopeType(), request.getScopeId())) {
            throw new BusinessException(MesBaseErrorCodes.USER_SCOPE_INVALID, "这个范围已经授过");
        }
        UserScope scope = new UserScope();
        scope.setUserName(request.getUserName());
        scope.setScopeType(request.getScopeType());
        scope.setScopeId(request.getScopeId());
        repository.save(scope);
    }

    /**
     * 用户能否操作这个对象。
     *
     * @param userName 用户
     * @param scopeType 类型
     * @param scopeId 对象
     * @return 没有该类型记录，或对象在名单内时为 true
     */
    public boolean allowed(String userName, String scopeType, Long scopeId) {
        if (!repository.existsType(userName, scopeType)) {
            return true;
        }
        return repository.exists(userName, scopeType, scopeId);
    }
}
