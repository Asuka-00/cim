package com.scaffold.mes.exec.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.exec.domain.entity.Handover;
import com.scaffold.mes.exec.domain.repository.HandoverRepository;
import com.scaffold.mes.exec.infra.mapper.db.HandoverMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * HandoverDbRepositoryImpl 交接班仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class HandoverDbRepositoryImpl implements HandoverRepository {

    private final HandoverMapper mapper;

    @Override
    public Optional<Handover> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public Optional<Handover> findByNo(String handoverNo) {
        return new DbRepositorySupport<>(mapper, Handover.class)
                .findOne(new LambdaQueryWrapper<Handover>().eq(Handover::getHandoverNo, handoverNo));
    }

    @Override
    public List<Handover> list() {
        return mapper.selectList(new LambdaQueryWrapper<Handover>().orderByAsc(Handover::getHandoverNo));
    }

    @Override
    public void save(Handover handover) {
        new DbRepositorySupport<>(mapper, Handover.class).save(handover);
    }

    @Override
    public void update(Handover handover) {
        new DbRepositorySupport<>(mapper, Handover.class).update(handover);
    }
}
