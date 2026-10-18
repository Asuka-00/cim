package com.scaffold.mes.exec.infra.repository.db;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.exec.domain.entity.FutureAction;
import com.scaffold.mes.exec.domain.repository.FutureActionRepository;
import com.scaffold.mes.exec.infra.mapper.db.FutureActionMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * FutureActionDbRepositoryImpl 预约动作仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class FutureActionDbRepositoryImpl implements FutureActionRepository {

    private final FutureActionMapper mapper;

    private DbRepositorySupport<FutureAction> support() {
        return new DbRepositorySupport<>(mapper, FutureAction.class);
    }

    @Override
    public void save(FutureAction action) {
        support().save(action);
    }

    @Override
    public void update(FutureAction action) {
        support().update(action);
    }

    @Override
    public List<FutureAction> listDue(Long lotId, Long routeVersionId, Integer routeSeqNo) {
        return mapper.selectList(new LambdaQueryWrapper<FutureAction>()
                .eq(FutureAction::getLotId, lotId)
                .eq(FutureAction::getRouteVersionId, routeVersionId)
                .eq(FutureAction::getRouteSeqNo, routeSeqNo)
                .eq(FutureAction::getStatus, "PENDING")
                .eq(FutureAction::getTiming, "ON_ARRIVE"));
    }

    @Override
    public List<FutureAction> listPending(Long lotId, Long routeVersionId) {
        return mapper.selectList(new LambdaQueryWrapper<FutureAction>()
                .eq(FutureAction::getLotId, lotId)
                .eq(FutureAction::getRouteVersionId, routeVersionId)
                .eq(FutureAction::getStatus, "PENDING"));
    }

    @Override
    public List<FutureAction> listByLot(Long lotId) {
        return mapper.selectList(new LambdaQueryWrapper<FutureAction>()
                .eq(FutureAction::getLotId, lotId)
                .orderByAsc(FutureAction::getRouteSeqNo));
    }
}
