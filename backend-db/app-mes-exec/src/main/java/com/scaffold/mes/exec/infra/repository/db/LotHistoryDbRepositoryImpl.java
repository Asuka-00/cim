package com.scaffold.mes.exec.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.exec.domain.entity.LotHistory;
import com.scaffold.mes.exec.domain.repository.LotHistoryRepository;
import com.scaffold.mes.exec.infra.mapper.db.LotHistoryMapper;
import com.scaffold.share.infra.repository.db.AppendOnlyRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * LotHistoryDbRepositoryImpl Lot 历史仓储实现
 * <p>
 * 只插入，不更新。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class LotHistoryDbRepositoryImpl implements LotHistoryRepository {

    private final LotHistoryMapper mapper;

    @Override
    public void save(LotHistory history) {
        new AppendOnlyRepositorySupport<>(mapper, LotHistory.class).save(history);
    }

    @Override
    public List<LotHistory> listByLot(Long lotId) {
        return mapper.selectList(new LambdaQueryWrapper<LotHistory>()
                .eq(LotHistory::getLotId, lotId)
                .orderByDesc(LotHistory::getOccurredAt));
    }

    @Override
    public int countEnterRework(Long rootLotId, Long reworkId) {
        return mapper.countEnterRework(rootLotId, reworkId);
    }

    @Override
    public Optional<LotHistory> findLatest(Long lotId, String txnType) {
        return new AppendOnlyRepositorySupport<>(mapper, LotHistory.class).findOne(new LambdaQueryWrapper<LotHistory>()
                .eq(LotHistory::getLotId, lotId)
                .eq(LotHistory::getTxnType, txnType)
                .orderByDesc(LotHistory::getOccurredAt)
                .orderByDesc(LotHistory::getId));
    }
}
