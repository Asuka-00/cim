package com.scaffold.mes.base.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.RouteStepSamplingSaveRequest;
import com.scaffold.mes.base.domain.entity.RouteStep;
import com.scaffold.mes.base.domain.entity.RouteStepSampling;
import com.scaffold.mes.base.domain.entity.SamplingRule;
import com.scaffold.mes.base.domain.repository.RouteStepRepository;
import com.scaffold.mes.base.domain.repository.RouteStepSamplingRepository;
import com.scaffold.mes.base.domain.repository.SamplingRuleRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * RouteStepSamplingApplicationService Route 步骤抽检覆盖
 * <p>
 * 只接受已启用的间隔规则。同一 Route 步骤与产品已有覆盖时拒绝。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class RouteStepSamplingApplicationService {

    private final RouteStepRepository routeSteps;
    private final RouteStepSamplingRepository samplings;
    private final SamplingRuleRepository rules;

    /**
     * 列出某个版本的抽检覆盖。
     *
     * @param routeVersionId Route 版本
     * @return 覆盖
     */
    public List<RouteStepSampling> list(Long routeVersionId) {
        return samplings.listByVersion(routeVersionId);
    }

    /**
     * 保存覆盖。
     *
     * @param request 版本、序号、产品和规则
     * @throws BusinessException 步骤不存在、规则不可用，或该步骤与产品已有覆盖
     */
    @Transactional
    public void save(RouteStepSamplingSaveRequest request) {
        RouteStep step = routeSteps.findBySeq(request.getRouteVersionId(), request.getSeqNo()).orElse(null);
        if (step == null) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_SAMPLING_INVALID, "Route 步骤不存在");
        }
        SamplingRule rule = rules.findById(request.getRuleId()).orElse(null);
        if (rule == null || !Integer.valueOf(1).equals(rule.getEnabled())
                || (!"EVERY_N_LOT".equals(rule.getSampleType()) && !"PERCENT".equals(rule.getSampleType())
                        && !"ALL".equals(rule.getSampleType()))) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_SAMPLING_INVALID, "抽检规则不可用");
        }
        long productId = request.getProductId() == null ? 0L : request.getProductId();
        boolean exists = samplings.listByRouteStep(step.getId()).stream()
                .anyMatch(row -> Long.valueOf(productId).equals(row.getProductId()));
        if (exists) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_SAMPLING_INVALID, "这个步骤与产品已有抽检覆盖");
        }
        RouteStepSampling row = new RouteStepSampling();
        row.setRouteVersionId(request.getRouteVersionId());
        row.setRouteStepId(step.getId());
        row.setProductId(productId);
        row.setRuleId(request.getRuleId());
        samplings.save(row);
    }
}
