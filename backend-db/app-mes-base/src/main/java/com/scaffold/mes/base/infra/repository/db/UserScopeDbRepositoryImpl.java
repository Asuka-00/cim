package com.scaffold.mes.base.infra.repository.db;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.base.domain.entity.UserScope;
import com.scaffold.mes.base.domain.repository.UserScopeRepository;
import com.scaffold.mes.base.infra.mapper.db.UserScopeMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * UserScopeDbRepositoryImpl 用户数据权限仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class UserScopeDbRepositoryImpl implements UserScopeRepository {

    private final UserScopeMapper mapper;

    @Override
    public boolean existsType(String userName, String scopeType) {
        return new DbRepositorySupport<>(mapper, UserScope.class)
                .findOne(new LambdaQueryWrapper<UserScope>()
                        .eq(UserScope::getUserName, userName)
                        .eq(UserScope::getScopeType, scopeType))
                .isPresent();
    }

    @Override
    public boolean exists(String userName, String scopeType, Long scopeId) {
        return new DbRepositorySupport<>(mapper, UserScope.class)
                .findOne(new LambdaQueryWrapper<UserScope>()
                        .eq(UserScope::getUserName, userName)
                        .eq(UserScope::getScopeType, scopeType)
                        .eq(UserScope::getScopeId, scopeId))
                .isPresent();
    }

    @Override
    public List<UserScope> list() {
        return mapper.selectList(new LambdaQueryWrapper<UserScope>().orderByAsc(UserScope::getUserName));
    }

    @Override
    public void save(UserScope scope) {
        new DbRepositorySupport<>(mapper, UserScope.class).save(scope);
    }
}
