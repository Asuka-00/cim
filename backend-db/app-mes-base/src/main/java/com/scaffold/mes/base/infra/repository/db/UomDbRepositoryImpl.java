package com.scaffold.mes.base.infra.repository.db;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.Uom;
import com.scaffold.mes.base.domain.repository.UomRepository;
import com.scaffold.mes.base.infra.mapper.db.UomMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * UomDbRepositoryImpl 物料计量单位仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_uom。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class UomDbRepositoryImpl implements UomRepository {

    private final UomMapper mapper;

    private DbRepositorySupport<Uom> support() {
        return new DbRepositorySupport<>(mapper, Uom.class);
    }

    @Override
    public Optional<Uom> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<Uom> findByCode(String uomCode) {
        return support().findOne(new LambdaQueryWrapper<Uom>().eq(Uom::getUomCode, uomCode));
    }

    @Override
    public PageDTO<Uom> page(int page, int pageSize, String uomCode) {
        LambdaQueryWrapper<Uom> wrapper = support().wrapper();
        if (uomCode != null && !uomCode.isBlank()) {
            wrapper.like(Uom::getUomCode, uomCode);
        }
        return support().queryPage(page, pageSize, wrapper);
    }

    @Override
    public void save(Uom uom) {
        support().save(uom);
    }

    @Override
    public void update(Uom uom) {
        support().update(uom);
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
