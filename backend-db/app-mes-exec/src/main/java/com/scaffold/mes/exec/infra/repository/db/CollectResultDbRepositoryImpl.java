package com.scaffold.mes.exec.infra.repository.db;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.exec.domain.entity.CollectResult;
import com.scaffold.mes.exec.domain.repository.CollectResultRepository;
import com.scaffold.mes.exec.infra.mapper.db.CollectResultMapper;
import com.scaffold.share.infra.repository.db.AppendOnlyRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * CollectResultDbRepositoryImpl 采集结果仓储实现
 * <p>
 * 只插入，不更新。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class CollectResultDbRepositoryImpl implements CollectResultRepository {

    private final CollectResultMapper mapper;

    @Override
    public void save(CollectResult result) {
        new AppendOnlyRepositorySupport<>(mapper, CollectResult.class).save(result);
    }

    @Override
    public Optional<CollectResult> latest(Long lotId, Long itemId, Integer routeSeqNo, Integer passNo) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<CollectResult>()
                .eq(CollectResult::getLotId, lotId)
                .eq(CollectResult::getItemId, itemId)
                .eq(CollectResult::getRouteSeqNo, routeSeqNo)
                .eq(CollectResult::getPassNo, passNo)
                .orderByDesc(CollectResult::getCollectedAt)
                .orderByDesc(CollectResult::getId)
                .last("LIMIT 1")));
    }
}
