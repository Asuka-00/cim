package com.scaffold.mes.exec.domain.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.entity.SamplingCounter;
import com.scaffold.mes.exec.domain.repository.SamplingCounterRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * SamplingCounterApplicationService 抽检计数
 * <p>
 * 每进一个 Lot 加 1。计数达到间隔 N 时抽中，并把计数清零。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class SamplingCounterApplicationService {

    private final SamplingCounterRepository repository;
    private final Clock clock;

    /**
     * 列出全部计数器。
     *
     * @return 按规则排列
     */
    public List<SamplingCounter> list() {
        return repository.list();
    }

    /**
     * 推进一次计数。
     *
     * @param ruleId 规则
     * @param stepId 站点
     * @param routeStepId Route 步骤。空按 0
     * @param productId 产品。空按 0
     * @param n 每 N 个 Lot 抽 1 个
     * @param lotId 当前 Lot
     * @return 本次抽中时为 true
     * @throws BusinessException 间隔小于 1
     */
    @Transactional
    public boolean advance(Long ruleId, Long stepId, Long routeStepId, Long productId, int n, Long lotId) {
        if (n < 1) {
            throw new BusinessException(MesExecErrorCodes.SAMPLING_INTERVAL, "抽检间隔至少为 1");
        }
        long route = routeStepId == null ? 0L : routeStepId;
        long product = productId == null ? 0L : productId;
        SamplingCounter row = repository.find(ruleId, stepId, route, product).orElse(null);
        boolean created = row == null;
        if (created) {
            row = new SamplingCounter();
            row.setRuleId(ruleId);
            row.setStepId(stepId);
            row.setRouteStepId(route);
            row.setProductId(product);
            row.setCounter(0);
        }
        int next = (row.getCounter() == null ? 0 : row.getCounter()) + 1;
        boolean hit = next >= n;
        if (hit) {
            row.setCounter(0);
            row.setLastSampledLotId(lotId);
            row.setLastSampledAt(LocalDateTime.ofInstant(clock.instant(), clock.getZone()));
        } else {
            row.setCounter(next);
        }
        if (created) {
            repository.save(row);
        } else {
            repository.update(row);
        }
        return hit;
    }

    /**
     * 按百分比推进一次。累计百分比达到 100 时抽中并清零。
     *
     * @param ruleId 规则
     * @param stepId 站点
     * @param routeStepId Route 步骤。空按 0
     * @param productId 产品。空按 0
     * @param percent 百分比，大于 0 且不超过 100
     * @param lotId 当前 Lot
     * @return 本次抽中时为 true
     * @throws BusinessException 百分比不在范围内
     */
    @Transactional
    public boolean advanceByPercent(Long ruleId, Long stepId, Long routeStepId, Long productId, BigDecimal percent,
            Long lotId) {
        if (percent == null || percent.signum() <= 0 || percent.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException(MesExecErrorCodes.SAMPLING_INTERVAL, "抽检百分比要在 0 和 100 之间");
        }
        long route = routeStepId == null ? 0L : routeStepId;
        long product = productId == null ? 0L : productId;
        SamplingCounter row = repository.find(ruleId, stepId, route, product).orElse(null);
        boolean created = row == null;
        if (created) {
            row = new SamplingCounter();
            row.setRuleId(ruleId);
            row.setStepId(stepId);
            row.setRouteStepId(route);
            row.setProductId(product);
            row.setCounter(0);
        }
        int next = (row.getCounter() == null ? 0 : row.getCounter()) + 1;
        boolean hit = percent.multiply(BigDecimal.valueOf(next)).compareTo(new BigDecimal("100")) >= 0;
        if (hit) {
            row.setCounter(0);
            row.setLastSampledLotId(lotId);
            row.setLastSampledAt(LocalDateTime.ofInstant(clock.instant(), clock.getZone()));
        } else {
            row.setCounter(next);
        }
        if (created) {
            repository.save(row);
        } else {
            repository.update(row);
        }
        return hit;
    }

    /**
     * 把计数清零。没有计数器时什么都不做。最近抽中的 Lot 保留。
     *
     * @param ruleId 规则
     * @param stepId 站点
     * @param routeStepId Route 步骤。空按 0
     * @param productId 产品。空按 0
     */
    @Transactional
    public void reset(Long ruleId, Long stepId, Long routeStepId, Long productId) {
        long route = routeStepId == null ? 0L : routeStepId;
        long product = productId == null ? 0L : productId;
        SamplingCounter row = repository.find(ruleId, stepId, route, product).orElse(null);
        if (row == null) {
            return;
        }
        row.setCounter(0);
        repository.update(row);
    }
}
