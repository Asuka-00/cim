package com.scaffold.mes.base.infra.repository.db;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.Area;
import com.scaffold.mes.base.domain.repository.AreaRepository;
import com.scaffold.mes.base.infra.mapper.db.AreaMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * AreaDbRepositoryImpl 区域仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_area。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class AreaDbRepositoryImpl implements AreaRepository {

    private final AreaMapper mapper;

    private DbRepositorySupport<Area> support() {
        return new DbRepositorySupport<>(mapper, Area.class);
    }

    @Override
    public Optional<Area> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<Area> findByCode(String areaCode) {
        return support().findOne(new LambdaQueryWrapper<Area>().eq(Area::getAreaCode, areaCode));
    }

    @Override
    public PageDTO<Area> page(int page, int pageSize, String areaCode) {
        LambdaQueryWrapper<Area> wrapper = support().wrapper();
        if (areaCode != null && !areaCode.isBlank()) {
            wrapper.like(Area::getAreaCode, areaCode);
        }
        return support().queryPage(page, pageSize, wrapper);
    }

    @Override
    public void save(Area area) {
        support().save(area);
    }

    @Override
    public void update(Area area) {
        support().update(area);
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
