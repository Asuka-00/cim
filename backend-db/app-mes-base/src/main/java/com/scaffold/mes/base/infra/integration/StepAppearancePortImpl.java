package com.scaffold.mes.base.infra.integration;

import org.springframework.stereotype.Component;

import com.scaffold.mes.base.domain.repository.RouteStepRepository;
import com.scaffold.share.integration.mes.base.provider.StepAppearancePort;

import lombok.RequiredArgsConstructor;

/**
 * StepAppearancePortImpl 站点出现次数
 * <p>
 * 按 Route 版本里步骤的站点计数。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
public class StepAppearancePortImpl implements StepAppearancePort {

    private final RouteStepRepository steps;

    @Override
    public int count(Long routeVersionId, Long stepId) {
        if (routeVersionId == null || stepId == null) {
            return 0;
        }
        return (int) steps.listByVersion(routeVersionId).stream()
                .filter(row -> stepId.equals(row.getStepId()))
                .count();
    }
}
