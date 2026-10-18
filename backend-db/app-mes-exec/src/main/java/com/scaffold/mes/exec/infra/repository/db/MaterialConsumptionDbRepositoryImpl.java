package com.scaffold.mes.exec.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.exec.domain.entity.MaterialConsumption;
import com.scaffold.mes.exec.domain.repository.MaterialConsumptionRepository;
import com.scaffold.mes.exec.infra.mapper.db.MaterialConsumptionMapper;
import com.scaffold.share.infra.repository.db.AppendOnlyRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * MaterialConsumptionDbRepositoryImpl 物料消耗仓储实现
 * <p>
 * 只插入，不更新。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class MaterialConsumptionDbRepositoryImpl implements MaterialConsumptionRepository {

    private final MaterialConsumptionMapper mapper;

    @Override
    public void save(MaterialConsumption consumption) {
        new AppendOnlyRepositorySupport<>(mapper, MaterialConsumption.class).save(consumption);
    }

    @Override
    public List<MaterialConsumption> listByLot(Long lotId) {
        return mapper.selectList(new LambdaQueryWrapper<MaterialConsumption>()
                .eq(MaterialConsumption::getLotId, lotId)
                .orderByAsc(MaterialConsumption::getOccurredAt)
                .orderByAsc(MaterialConsumption::getId));
    }

    @Override
    public Optional<MaterialConsumption> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public boolean existsSettle(Long debtId) {
        return mapper.selectCount(new LambdaQueryWrapper<MaterialConsumption>()
                .eq(MaterialConsumption::getAdjustRefId, debtId)
                .eq(MaterialConsumption::getSource, "SETTLE")) > 0;
    }
}
