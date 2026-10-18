package com.scaffold.mes.exec.infra.repository.db;

import org.springframework.stereotype.Repository;

import com.scaffold.mes.exec.domain.entity.UnitConversion;
import com.scaffold.mes.exec.domain.repository.UnitConversionRepository;
import com.scaffold.mes.exec.infra.mapper.db.UnitConversionMapper;
import com.scaffold.share.infra.repository.db.AppendOnlyRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * UnitConversionDbRepositoryImpl 单位转换历史仓储实现
 * <p>
 * 只插入，不更新。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class UnitConversionDbRepositoryImpl implements UnitConversionRepository {

    private final UnitConversionMapper mapper;

    @Override
    public void save(UnitConversion conversion) {
        new AppendOnlyRepositorySupport<>(mapper, UnitConversion.class).save(conversion);
    }
}
