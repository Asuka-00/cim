package com.scaffold.mes.exec.infra.repository.db;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.exec.domain.entity.LotTimeLinkTimer;
import com.scaffold.mes.exec.domain.repository.LotTimeLinkTimerRepository;
import com.scaffold.mes.exec.infra.mapper.db.LotTimeLinkTimerMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * LotTimeLinkTimerDbRepositoryImpl 时间链计时仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class LotTimeLinkTimerDbRepositoryImpl implements LotTimeLinkTimerRepository {

    private final LotTimeLinkTimerMapper mapper;

    @Override
    public void save(LotTimeLinkTimer timer) {
        new DbRepositorySupport<>(mapper, LotTimeLinkTimer.class).save(timer);
    }

    @Override
    public List<LotTimeLinkTimer> listActive(Long lotId) {
        return mapper.selectList(new LambdaQueryWrapper<LotTimeLinkTimer>()
                .eq(LotTimeLinkTimer::getLotId, lotId)
                .eq(LotTimeLinkTimer::getStatus, "ACTIVE"));
    }

    @Override
    public List<LotTimeLinkTimer> listOverdue(LocalDateTime now) {
        return mapper.selectList(new LambdaQueryWrapper<LotTimeLinkTimer>()
                .eq(LotTimeLinkTimer::getStatus, "ACTIVE")
                .lt(LotTimeLinkTimer::getDeadlineAt, now)
                .ne(LotTimeLinkTimer::getAlertLevel, "BREACHED"));
    }

    @Override
    public void update(LotTimeLinkTimer timer) {
        new DbRepositorySupport<>(mapper, LotTimeLinkTimer.class).update(timer);
    }
}
