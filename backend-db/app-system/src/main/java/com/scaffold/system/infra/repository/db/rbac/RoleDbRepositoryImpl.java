package com.scaffold.system.infra.repository.db.rbac;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.share.enums.StateEnum;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;
import com.scaffold.system.domain.entity.rbac.Role;
import com.scaffold.system.domain.entity.rbac.UserRole;
import com.scaffold.system.domain.query.rbac.RoleQuery;
import com.scaffold.system.domain.repository.rbac.RoleRepository;
import com.scaffold.system.domain.repository.rbac.UserRoleRepository;
import com.scaffold.system.infra.mapper.db.rbac.RoleMapper;

import lombok.RequiredArgsConstructor;

/**
 * RoleDbRepositoryImpl implements RoleRepository DB 热库仓储实现。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-05-28
 */
@Repository
@RequiredArgsConstructor
public class RoleDbRepositoryImpl implements RoleRepository {

    private final RoleMapper roleMapper;
    private final UserRoleRepository userRoleRepository;

    private DbRepositorySupport<Role> support() {
        return new DbRepositorySupport<>(roleMapper, Role.class);
    }

    @Override
    public Optional<Role> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public List<Role> findAllById(List<Long> ids) {
        return support().findByIds(ids);
    }

    @Override
    public Optional<Role> findByRoleCode(String roleCode) {
        if (isBlank(roleCode))
            return Optional.empty();
        return support().findOne(support().wrapper().eq(Role::getRoleCode, roleCode));
    }

    @Override
    public List<Role> findAll() {
        return support().findAll();
    }

    @Override
    public List<Role> findActiveAll() {
        return support().queryAll(support().wrapper()
                .eq(Role::getEnabled, StateEnum.ENABLED));
    }

    @Override
    public List<Role> findAllByTenantId(String tenantId) {
        if (isBlank(tenantId))
            return List.of();
        return support().queryAll(support().wrapper().eq(Role::getTenantId, tenantId));
    }

    @Override
    public void save(Role role) {
        support().save(role);
    }

    @Override
    public void update(Role role) {
        support().update(role);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }

    @Override
    public List<Role> findByUserId(Long userId) {
        Set<Long> roleIds = userRoleRepository.findByUserId(userId).stream().map(UserRole::getRoleId)
                .collect(Collectors.toSet());
        return support().findByIds(roleIds);
    }

    @Override
    public PageDTO<Role> queryRolePage(int page, int pageSize, RoleQuery query) {
        LambdaQueryWrapper<Role> wrapper = support().wrapper();
        if (query != null) {
            wrapper.eq(hasText(query.getRoleCode()), Role::getRoleCode, query.getRoleCode())
                    .like(hasText(query.getRoleName()), Role::getRoleName, query.getRoleName())
                    .eq(query.getEnabled() != null, Role::getEnabled, query.getEnabled());
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
