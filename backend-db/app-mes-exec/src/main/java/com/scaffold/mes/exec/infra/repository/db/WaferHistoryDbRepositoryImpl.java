package com.scaffold.mes.exec.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.exec.domain.entity.WaferHistory;
import com.scaffold.mes.exec.domain.repository.WaferHistoryRepository;
import com.scaffold.mes.exec.infra.mapper.db.WaferHistoryMapper;
import com.scaffold.share.infra.repository.db.AppendOnlyRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * WaferHistoryDbRepositoryImpl Wafer 历史仓储实现
 * <p>
 * 只插入，不更新。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class WaferHistoryDbRepositoryImpl implements WaferHistoryRepository {

    private final WaferHistoryMapper mapper;

    @Override
    public void save(WaferHistory history) {
        new AppendOnlyRepositorySupport<>(mapper, WaferHistory.class).save(history);
    }

    @Override
    public List<WaferHistory> listByWafer(Long waferId) {
        return mapper.selectList(new LambdaQueryWrapper<WaferHistory>()
                .eq(WaferHistory::getWaferId, waferId)
                .orderByDesc(WaferHistory::getOccurredAt));
    }

    @Override
    public Optional<WaferHistory> findLatest(Long waferId, String txnType) {
        return new AppendOnlyRepositorySupport<>(mapper, WaferHistory.class).findOne(new LambdaQueryWrapper<WaferHistory>()
                .eq(WaferHistory::getWaferId, waferId)
                .eq(WaferHistory::getTxnType, txnType)
                .orderByDesc(WaferHistory::getOccurredAt)
                .orderByDesc(WaferHistory::getId));
    }
}
