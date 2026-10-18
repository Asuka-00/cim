package com.scaffold.mes.core.infra.repository.db.alert;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.core.domain.entity.alert.MesAlert;
import com.scaffold.mes.core.domain.repository.alert.MesAlertRepository;
import com.scaffold.mes.core.infra.mapper.db.alert.MesAlertMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * MesAlertDbRepositoryImpl 预警仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_alert。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class MesAlertDbRepositoryImpl implements MesAlertRepository {

    private final MesAlertMapper mapper;

    private DbRepositorySupport<MesAlert> support() {
        return new DbRepositorySupport<>(mapper, MesAlert.class);
    }

    @Override
    public Optional<MesAlert> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<MesAlert> findByEventAndRule(Long eventId, String ruleCode) {
        return support().findOne(new LambdaQueryWrapper<MesAlert>()
                .eq(MesAlert::getEventId, eventId)
                .eq(MesAlert::getRuleCode, ruleCode));
    }

    @Override
    public PageDTO<MesAlert> page(int page, int pageSize, String status) {
        LambdaQueryWrapper<MesAlert> wrapper = support().wrapper();
        if (status != null && !status.isBlank()) {
            wrapper.eq(MesAlert::getStatus, status);
        }
        return support().queryPage(page, pageSize, wrapper);
    }

    @Override
    public long countOpen() {
        Long count = mapper.selectCount(new LambdaQueryWrapper<MesAlert>().in(MesAlert::getStatus, "NEW", "READ"));
        return count == null ? 0 : count;
    }

    @Override
    public void save(MesAlert alert) {
        support().save(alert);
    }

    @Override
    public void update(MesAlert alert) {
        mapper.updateById(alert);
    }
}
