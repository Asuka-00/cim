package com.scaffold.mes.exec.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.exec.domain.entity.LotHold;
import com.scaffold.mes.exec.domain.repository.LotHoldRepository;
import com.scaffold.mes.exec.infra.mapper.db.LotHoldMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * LotHoldDbRepositoryImpl Lot Hold 仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_lot_hold。该表没有乐观锁列。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class LotHoldDbRepositoryImpl implements LotHoldRepository {

    private final LotHoldMapper mapper;

    private DbRepositorySupport<LotHold> support() {
        return new DbRepositorySupport<>(mapper, LotHold.class);
    }

    @Override
    public List<LotHold> listActive(Long lotId) {
        return mapper.selectList(new LambdaQueryWrapper<LotHold>()
                .eq(LotHold::getLotId, lotId)
                .eq(LotHold::getStatus, "ACTIVE"));
    }

    @Override
    public Optional<LotHold> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<LotHold> findActiveByKey(String activeKey) {
        return support().findOne(new LambdaQueryWrapper<LotHold>()
                .eq(LotHold::getActiveKey, activeKey)
                .eq(LotHold::getStatus, "ACTIVE"));
    }

    @Override
    public void save(LotHold hold) {
        support().save(hold);
    }

    @Override
    public void update(LotHold hold) {
        support().update(hold);
    }
}
