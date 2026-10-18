package com.scaffold.mes.exec.infra.repository.db;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.exec.domain.entity.HoldReason;
import com.scaffold.mes.exec.domain.repository.HoldReasonRepository;
import com.scaffold.mes.exec.infra.mapper.db.HoldReasonMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * HoldReasonDbRepositoryImpl Hold 原因仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_hold_reason。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class HoldReasonDbRepositoryImpl implements HoldReasonRepository {

    private final HoldReasonMapper mapper;

    private DbRepositorySupport<HoldReason> support() {
        return new DbRepositorySupport<>(mapper, HoldReason.class);
    }

    @Override
    public Optional<HoldReason> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<HoldReason> findByCode(String reasonCode) {
        return support().findOne(new LambdaQueryWrapper<HoldReason>().eq(HoldReason::getReasonCode, reasonCode));
    }

    @Override
    public PageDTO<HoldReason> page(int page, int pageSize, String reasonCode) {
        LambdaQueryWrapper<HoldReason> wrapper = support().wrapper();
        if (reasonCode != null && !reasonCode.isBlank()) {
            wrapper.like(HoldReason::getReasonCode, reasonCode);
        }
        return support().queryPage(page, pageSize, wrapper.orderByAsc(HoldReason::getReasonCode));
    }

    @Override
    public void save(HoldReason reason) {
        support().save(reason);
    }

    @Override
    public void update(HoldReason reason) {
        support().update(reason);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }

    @Override
    public long countHolds(String reasonCode) {
        return mapper.countHolds(reasonCode);
    }
}
