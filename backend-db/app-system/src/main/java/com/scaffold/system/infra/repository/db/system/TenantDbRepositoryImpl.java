package com.scaffold.system.infra.repository.db.system;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.share.enums.StateEnum;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;
import com.scaffold.system.domain.entity.system.Tenant;
import com.scaffold.system.domain.query.system.TenantQuery;
import com.scaffold.system.domain.repository.system.TenantRepository;
import com.scaffold.system.infra.mapper.db.system.TenantMapper;

import lombok.RequiredArgsConstructor;

/**
 * TenantDbRepositoryImpl implements TenantRepository DB 热库仓储实现。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-05-28
 */
@Repository
@RequiredArgsConstructor
public class TenantDbRepositoryImpl implements TenantRepository {

    private final TenantMapper mapper;

    private DbRepositorySupport<Tenant> support() {
        return new DbRepositorySupport<>(mapper, Tenant.class);
    }

    @Override
    public Optional<Tenant> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<Tenant> findByTenantId(String tenantId) {
        if (isBlank(tenantId))
            return Optional.empty();
        return support().findOne(support().wrapper().eq(Tenant::getTenantId, tenantId));
    }

    @Override
    public List<Tenant> findAll() {
        return support().findAll();
    }

    @Override
    public List<Tenant> findActiveAll() {
        return support().queryAll(support().wrapper()
                .eq(Tenant::getEnabled, StateEnum.ENABLED));
    }

    @Override
    public void save(Tenant entity) {
        support().save(entity);
    }

    @Override
    public void update(Tenant entity) {
        support().update(entity);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }

    @Override
    public PageDTO<Tenant> queryTenantPage(int page, int pageSize, TenantQuery query) {
        LambdaQueryWrapper<Tenant> wrapper = support().wrapper();
        if (query != null) {
            wrapper.eq(hasText(query.getTenantId()), Tenant::getTenantId, query.getTenantId())
                    .like(hasText(query.getTenantName()), Tenant::getTenantName, query.getTenantName())
                    .eq(query.getEnabled() != null, Tenant::getEnabled, query.getEnabled());
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
