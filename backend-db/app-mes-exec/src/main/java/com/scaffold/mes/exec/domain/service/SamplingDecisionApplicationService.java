package com.scaffold.mes.exec.domain.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.exec.domain.entity.LotSamplingPlan;
import com.scaffold.mes.exec.domain.repository.LotSamplingPlanRepository;

import lombok.RequiredArgsConstructor;

/**
 * SamplingDecisionApplicationService 抽检决定
 * <p>
 * 已有决定时沿用，不再推进计数。没有决定时按间隔计数，抽中和没抽中都落一行。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class SamplingDecisionApplicationService {

    private final LotSamplingPlanRepository plans;
    private final SamplingCounterApplicationService counters;
    private final Clock clock;

    /**
     * 决定这个 Lot 这次经过要不要量测。
     *
     * @param lotId Lot
     * @param routeSeqNo 量测步骤序号
     * @param passNo 经过次数
     * @param stepId 量测站点
     * @param ruleId 规则
     * @param routeStepId Route 步骤。空按 0
     * @param productId 产品。空按 0
     * @param n 每 N 个 Lot 抽 1 个
     * @return 需要量测时为 true
     */
    @Transactional
    public boolean decide(Long lotId, Integer routeSeqNo, Integer passNo, Long stepId, Long ruleId,
            Long routeStepId, Long productId, int n, Integer waferCount, String slotList) {
        LotSamplingPlan existing = plans.find(lotId, routeSeqNo, passNo).orElse(null);
        if (existing != null) {
            return Integer.valueOf(1).equals(existing.getNeedMeasure());
        }
        boolean hit = counters.advance(ruleId, stepId, routeStepId, productId, n, lotId);
        LotSamplingPlan plan = new LotSamplingPlan();
        plan.setLotId(lotId);
        plan.setRouteSeqNo(routeSeqNo);
        plan.setPassNo(passNo);
        plan.setStepId(stepId);
        plan.setRuleId(ruleId);
        plan.setNeedMeasure(hit ? 1 : 0);
        plan.setWaferCount(hit ? waferCount : null);
        plan.setWaferSlots(hit ? slotList : null);
        plan.setDecidedAt(LocalDateTime.ofInstant(clock.instant(), clock.getZone()));
        plans.save(plan);
        return hit;
    }

    /**
     * 按百分比决定要不要量测。已有决定时沿用。
     *
     * @param lotId Lot
     * @param routeSeqNo 量测步骤序号
     * @param passNo 经过次数
     * @param stepId 量测站点
     * @param ruleId 规则
     * @param routeStepId Route 步骤。空按 0
     * @param productId 产品。空按 0
     * @param percent 百分比
     * @return 需要量测时为 true
     */
    @Transactional
    public boolean decideByPercent(Long lotId, Integer routeSeqNo, Integer passNo, Long stepId, Long ruleId,
            Long routeStepId, Long productId, BigDecimal percent, Integer waferCount, String slotList) {
        LotSamplingPlan existing = plans.find(lotId, routeSeqNo, passNo).orElse(null);
        if (existing != null) {
            return Integer.valueOf(1).equals(existing.getNeedMeasure());
        }
        boolean hit = counters.advanceByPercent(ruleId, stepId, routeStepId, productId, percent, lotId);
        LotSamplingPlan plan = new LotSamplingPlan();
        plan.setLotId(lotId);
        plan.setRouteSeqNo(routeSeqNo);
        plan.setPassNo(passNo);
        plan.setStepId(stepId);
        plan.setRuleId(ruleId);
        plan.setNeedMeasure(hit ? 1 : 0);
        plan.setWaferCount(hit ? waferCount : null);
        plan.setWaferSlots(hit ? slotList : null);
        plan.setDecidedAt(LocalDateTime.ofInstant(clock.instant(), clock.getZone()));
        plans.save(plan);
        return hit;
    }
}
