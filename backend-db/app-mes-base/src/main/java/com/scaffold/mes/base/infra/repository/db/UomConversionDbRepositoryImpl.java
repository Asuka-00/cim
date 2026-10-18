package com.scaffold.mes.base.infra.repository.db;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.UomConversion;
import com.scaffold.mes.base.domain.repository.UomConversionRepository;
import com.scaffold.mes.base.infra.mapper.db.UomConversionMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * UomConversionDbRepositoryImpl 物料单位换算仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_uom_conversion。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class UomConversionDbRepositoryImpl implements UomConversionRepository {

    private final UomConversionMapper mapper;

    private DbRepositorySupport<UomConversion> support() {
        return new DbRepositorySupport<>(mapper, UomConversion.class);
    }

    @Override
    public Optional<UomConversion> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<UomConversion> findExact(Long fromUomId, Long toUomId, Long materialId) {
        return support().findOne(new LambdaQueryWrapper<UomConversion>()
                .eq(UomConversion::getFromUomId, fromUomId)
                .eq(UomConversion::getToUomId, toUomId)
                .eq(UomConversion::getMaterialId, materialId));
    }

    @Override
    public PageDTO<UomConversion> page(int page, int pageSize, Long fromUomId) {
        LambdaQueryWrapper<UomConversion> wrapper = support().wrapper();
        if (fromUomId != null) {
            wrapper.eq(UomConversion::getFromUomId, fromUomId);
        }
        return support().queryPage(page, pageSize, wrapper);
    }

    @Override
    public void save(UomConversion conversion) {
        support().save(conversion);
    }

    @Override
    public void update(UomConversion conversion) {
        support().update(conversion);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }
}
