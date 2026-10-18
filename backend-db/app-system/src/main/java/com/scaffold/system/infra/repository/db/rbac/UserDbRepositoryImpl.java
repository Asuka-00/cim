package com.scaffold.system.infra.repository.db.rbac;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.enums.StateEnum;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;
import com.scaffold.system.domain.entity.rbac.User;
import com.scaffold.system.domain.query.rbac.UserQuery;
import com.scaffold.system.domain.repository.rbac.UserRepository;
import com.scaffold.system.domain.repository.rbac.UserTenantRepository;
import com.scaffold.system.infra.mapper.db.rbac.UserMapper;

import lombok.RequiredArgsConstructor;

/**
 * UserDbRepositoryImpl implements UserRepository DB 热库仓储实现。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-05-28
 */
@Repository
@RequiredArgsConstructor
public class UserDbRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;
    private final UserTenantRepository userTenantRepository;
    private final CurrentSessionProvider currentSessionProvider;

    private DbRepositorySupport<User> support() {
        return new DbRepositorySupport<>(userMapper, User.class);
    }

    @Override
    public Optional<User> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public List<User> findByIds(Set<Long> ids) {
        return support().findByIds(ids);
    }

    @Override
    public Optional<User> findByUserName(String userName) {
        if (isBlank(userName)) {
            return Optional.empty();
        }
        return support().findOne(support().wrapper().eq(User::getUserName, userName));
    }

    @Override
    public Optional<User> selectByUserNameWithTenant(String userName, String tenantId) {
        return findByUserName(userName);
    }

    @Override
    public List<User> findAll() {
        return support().findAll();
    }

    @Override
    public List<User> findActiveAll() {
        return support().queryAll(support().wrapper()
                .eq(User::getEnabled, StateEnum.ENABLED));
    }

    @Override
    public List<User> findAllByTenantId(String tenantId) {
        List<Long> userIds = userTenantRepository.findUserIdsByTenantId(tenantId);
        if (userIds.isEmpty()) {
            return List.of();
        }
        return support().findByIds(new HashSet<>(userIds));
    }

    @Override
    public long countByTenantId(String tenantId) {
        return userTenantRepository.countByTenantId(tenantId);
    }

    @Override
    public void save(User user) {
        support().save(user);
    }

    @Override
    public void update(User user) {
        support().update(user);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }

    @Override
    public PageDTO<User> queryUserPage(int page, int pageSize, UserQuery query) {
        String tenantId = currentSessionProvider.getCurrentTenantIdOrDefault();
        List<Long> userIds = userTenantRepository.findUserIdsByTenantId(tenantId);
        if (userIds.isEmpty()) {
            PageDTO<User> empty = new PageDTO<>();
            empty.setRecords(List.of());
            empty.setTotal(0);
            empty.setSize(Math.max(pageSize, 1));
            empty.setCurrent(Math.max(page, 1));
            return empty;
        }
        LambdaQueryWrapper<User> wrapper = support().wrapper();
        wrapper.in(User::getId, userIds);
        if (query != null) {
            wrapper.like(hasText(query.getUserName()), User::getUserName, query.getUserName())
                    .like(hasText(query.getDisplayName()), User::getDisplayName, query.getDisplayName())
                    .eq(query.getEnabled() != null, User::getEnabled, query.getEnabled());
        }
        return support().queryPage(page, pageSize, wrapper);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

}
