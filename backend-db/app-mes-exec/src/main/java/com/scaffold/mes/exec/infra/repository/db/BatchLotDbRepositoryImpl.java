package com.scaffold.mes.exec.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.exec.domain.entity.BatchLot;
import com.scaffold.mes.exec.domain.repository.BatchLotRepository;
import com.scaffold.mes.exec.infra.mapper.db.BatchLotMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * BatchLotDbRepositoryImpl 批次成员仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class BatchLotDbRepositoryImpl implements BatchLotRepository {

    private final BatchLotMapper mapper;

    private DbRepositorySupport<BatchLot> support() {
        return new DbRepositorySupport<>(mapper, BatchLot.class);
    }

    @Override
    public List<BatchLot> listIn(Long batchId) {
        return mapper.selectList(new LambdaQueryWrapper<BatchLot>()
                .eq(BatchLot::getBatchId, batchId)
                .eq(BatchLot::getStatus, "IN")
                .orderByAsc(BatchLot::getSeq));
    }

    @Override
    public Optional<BatchLot> find(Long batchId, Long lotId) {
        return support().findOne(new LambdaQueryWrapper<BatchLot>()
                .eq(BatchLot::getBatchId, batchId)
                .eq(BatchLot::getLotId, lotId));
    }

    @Override
    public void save(BatchLot member) {
        support().save(member);
    }

    @Override
    public void update(BatchLot member) {
        support().update(member);
    }
}
