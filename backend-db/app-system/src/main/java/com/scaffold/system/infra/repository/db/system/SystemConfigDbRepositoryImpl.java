package com.scaffold.system.infra.repository.db.system;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;
import com.scaffold.system.domain.entity.system.SystemConfig;
import com.scaffold.system.domain.query.system.SystemConfigQuery;
import com.scaffold.system.domain.repository.system.SystemConfigRepository;
import com.scaffold.system.infra.mapper.db.system.SystemConfigMapper;

import lombok.RequiredArgsConstructor;

/**
 * SystemConfigDbRepositoryImpl implements SystemConfigRepository DB 热库仓储实现。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-05-28
 */
@Repository
@RequiredArgsConstructor
public class SystemConfigDbRepositoryImpl implements SystemConfigRepository {

    private final SystemConfigMapper mapper;

    private DbRepositorySupport<SystemConfig> support() {
        return new DbRepositorySupport<>(mapper, SystemConfig.class);
    }

    @Override
    public Optional<SystemConfig> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<SystemConfig> findByConfigCode(String configCode) {
        if (isBlank(configCode))
            return Optional.empty();
        return support().findOne(support().wrapper().eq(SystemConfig::getConfigCode, configCode));
    }

    @Override
    public List<SystemConfig> findAll() {
        return support().findAll();
    }

    @Override
    public List<SystemConfig> findAllByTenantId(String tenantId) {
        if (isBlank(tenantId))
            return List.of();
        return support().queryAll(support().wrapper().eq(SystemConfig::getTenantId, tenantId));
    }

    @Override
    public void save(SystemConfig entity) {
        support().save(entity);
    }

    @Override
    public void update(SystemConfig entity) {
        support().update(entity);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }

    @Override
    public List<SystemConfig> querySystemConfigList(SystemConfigQuery query) {
        LambdaQueryWrapper<SystemConfig> wrapper = support().wrapper();
        if (query != null) {
            wrapper.eq(hasText(query.getConfigCode()), SystemConfig::getConfigCode, query.getConfigCode())
                    .eq(hasText(query.getGroupCode()), SystemConfig::getGroupCode, query.getGroupCode())
                    .eq(hasText(query.getConfigValue()), SystemConfig::getConfigValue, query.getConfigValue())
                    .eq(hasText(query.getValueType()), SystemConfig::getValueType, query.getValueType())
                    .eq(query.getState() != null, SystemConfig::getState, query.getState());
        }
        return support().queryAll(wrapper);
    }

    @Override
    public List<SystemConfig> findAllByGroupCodeIgnoreTenant(String groupCode) {
        if (isBlank(groupCode)) {
            return List.of();
        }
        return mapper.selectByGroupCodeIgnoreTenant(groupCode);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

}
