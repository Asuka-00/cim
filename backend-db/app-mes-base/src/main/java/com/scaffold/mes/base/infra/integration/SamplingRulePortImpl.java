package com.scaffold.mes.base.infra.integration;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.scaffold.mes.base.domain.entity.RouteStep;
import com.scaffold.mes.base.domain.entity.RouteStepSampling;
import com.scaffold.mes.base.domain.entity.SamplingRule;
import com.scaffold.mes.base.domain.entity.StepSampling;
import com.scaffold.mes.base.domain.repository.RouteStepRepository;
import com.scaffold.mes.base.domain.repository.RouteStepSamplingRepository;
import com.scaffold.mes.base.domain.repository.SamplingRuleRepository;
import com.scaffold.mes.base.domain.repository.StepSamplingRepository;
import com.scaffold.share.integration.mes.base.provider.SamplingRulePort;
import com.scaffold.share.integration.mes.base.response.SamplingBinding;

import lombok.RequiredArgsConstructor;

/**
 * SamplingRulePortImpl 站点抽检规则
 * <p>
 * 步骤上有覆盖时整组替换站点绑定。只返回启用的 EVERY_N_LOT。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
public class SamplingRulePortImpl implements SamplingRulePort {

    private final StepSamplingRepository bindings;
    private final RouteStepRepository routeSteps;
    private final RouteStepSamplingRepository routeSamplings;
    private final SamplingRuleRepository rules;

    @Override
    public Optional<SamplingBinding> binding(Long stepId, Long productId) {
        long product = productId == null ? 0L : productId;
        StepSampling binding = bindings.find(stepId, product).orElse(null);
        if (binding == null && product != 0L) {
            binding = bindings.find(stepId, 0L).orElse(null);
        }
        if (binding == null) {
            return Optional.empty();
        }
        return enabled(binding.getRuleId());
    }

    @Override
    public Optional<SamplingBinding> binding(Long routeVersionId, Integer seqNo, Long stepId, Long productId) {
        RouteStep step = routeVersionId == null || seqNo == null
                ? null
                : routeSteps.findBySeq(routeVersionId, seqNo).orElse(null);
        if (step != null) {
            List<RouteStepSampling> rows = routeSamplings.listByRouteStep(step.getId());
            if (!rows.isEmpty()) {
                return enabled(match(rows, productId));
            }
        }
        return binding(stepId, productId);
    }

    private static Long match(List<RouteStepSampling> rows, Long productId) {
        long product = productId == null ? 0L : productId;
        RouteStepSampling hit = rows.stream()
                .filter(row -> Long.valueOf(product).equals(row.getProductId()))
                .findFirst()
                .orElse(null);
        if (hit == null && product != 0L) {
            hit = rows.stream()
                    .filter(row -> row.getProductId() == null || row.getProductId() == 0L)
                    .findFirst()
                    .orElse(null);
        }
        return hit == null ? null : hit.getRuleId();
    }

    private Optional<SamplingBinding> enabled(Long ruleId) {
        if (ruleId == null) {
            return Optional.empty();
        }
        SamplingRule rule = rules.findById(ruleId).orElse(null);
        if (rule == null || !Integer.valueOf(1).equals(rule.getEnabled())) {
            return Optional.empty();
        }
        if ("EVERY_N_LOT".equals(rule.getSampleType()) && rule.getNValue() != null && rule.getNValue() >= 1) {
            return Optional.of(new SamplingBinding(rule.getId(), rule.getNValue(), "EVERY_N_LOT", null,
                    rule.getWaferCount(), rule.getSlotList()));
        }
        if ("ALL".equals(rule.getSampleType())) {
            return Optional.of(new SamplingBinding(rule.getId(), 1, "ALL", null, rule.getWaferCount(), rule.getSlotList()));
        }
        if ("PERCENT".equals(rule.getSampleType()) && rule.getPercent() != null && rule.getPercent().signum() > 0
                && rule.getPercent().compareTo(new java.math.BigDecimal("100")) <= 0) {
            return Optional.of(new SamplingBinding(rule.getId(), 0, "PERCENT", rule.getPercent(), rule.getWaferCount(),
                    rule.getSlotList()));
        }
        return Optional.empty();
    }
}
