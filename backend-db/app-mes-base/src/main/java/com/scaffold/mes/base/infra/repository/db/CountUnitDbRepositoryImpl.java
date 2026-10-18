package com.scaffold.mes.base.infra.repository.db;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.CountUnit;
import com.scaffold.mes.base.domain.repository.CountUnitRepository;
import com.scaffold.mes.base.infra.mapper.db.CountUnitMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * CountUnitDbRepositoryImpl 计数单位仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_count_unit。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class CountUnitDbRepositoryImpl implements CountUnitRepository {

    private final CountUnitMapper mapper;

    private DbRepositorySupport<CountUnit> support() {
        return new DbRepositorySupport<>(mapper, CountUnit.class);
    }

    @Override
    public Optional<CountUnit> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<CountUnit> findByCode(String unitCode) {
        return support().findOne(new LambdaQueryWrapper<CountUnit>().eq(CountUnit::getUnitCode, unitCode));
    }

    @Override
    public PageDTO<CountUnit> page(int page, int pageSize, String unitCode) {
        LambdaQueryWrapper<CountUnit> wrapper = support().wrapper();
        if (unitCode != null && !unitCode.isBlank()) {
            wrapper.like(CountUnit::getUnitCode, unitCode);
        }
        return support().queryPage(page, pageSize, wrapper);
    }

    @Override
    public void save(CountUnit unit) {
        support().save(unit);
    }

    @Override
    public void update(CountUnit unit) {
        support().update(unit);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }

    @Override
    public long countReferences(Long id) {
        return mapper.countReferences(id);
    }
}
