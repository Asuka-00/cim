package com.scaffold.mes.base.infra.integration;

import java.util.Comparator;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.scaffold.mes.base.domain.entity.RouteRework;
import com.scaffold.mes.base.domain.entity.RouteReworkStep;
import com.scaffold.mes.base.domain.entity.RouteStep;
import com.scaffold.mes.base.domain.repository.RouteReworkRepository;
import com.scaffold.mes.base.domain.repository.RouteReworkStepRepository;
import com.scaffold.mes.base.domain.repository.RouteStepRepository;
import com.scaffold.mes.base.domain.repository.StepRepository;
import com.scaffold.share.integration.mes.base.provider.ReworkRouteProvider;
import com.scaffold.share.integration.mes.base.response.ReworkEntry;
import com.scaffold.share.integration.mes.base.response.ReworkProgress;

import lombok.RequiredArgsConstructor;

/**
 * ReworkRouteProviderImpl 返工分支查询
 * <p>
 * 带上序号最小的返工步骤，作为进入后的第一站。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
public class ReworkRouteProviderImpl implements ReworkRouteProvider {

    private final RouteReworkRepository reworkRepository;
    private final RouteReworkStepRepository stepRepository;
    private final RouteStepRepository routeStepRepository;
    private final StepRepository mainStepRepository;

    @Override
    public Optional<ReworkEntry> find(Long reworkId) {
        return reworkRepository.findById(reworkId).map(rework -> {
            RouteReworkStep first = stepRepository.listByRework(reworkId).stream()
                    .filter(step -> step.getSeqNo() != null)
                    .min(Comparator.comparing(RouteReworkStep::getSeqNo))
                    .orElse(null);
            return toEntry(rework, first);
        });
    }

    @Override
    public Optional<ReworkProgress> nextAfter(Long reworkId, Integer currentReworkSeq) {
        return reworkRepository.findById(reworkId).map(rework -> {
            int current = currentReworkSeq == null ? 0 : currentReworkSeq;
            RouteReworkStep next = stepRepository.listByRework(reworkId).stream()
                    .filter(step -> step.getSeqNo() != null && step.getSeqNo() > current)
                    .min(Comparator.comparing(RouteReworkStep::getSeqNo))
                    .orElse(null);
            if (next != null) {
                return new ReworkProgress(false, next.getSeqNo(), null, next.getStepId(), null);
            }
            RouteStep back = routeStepRepository.listByVersion(rework.getRouteVersionId()).stream()
                    .filter(step -> rework.getReturnSeqNo().equals(step.getSeqNo()))
                    .findFirst()
                    .orElse(null);
            Long area = back == null ? null : mainStepRepository.findById(back.getStepId()).map(step -> step.getAreaId()).orElse(null);
            return new ReworkProgress(true, null, rework.getReturnSeqNo(), back == null ? null : back.getStepId(), area);
        });
    }

    private static ReworkEntry toEntry(RouteRework rework, RouteReworkStep first) {
        return new ReworkEntry(rework.getId(), rework.getRouteVersionId(),
                rework.getEntryFromSeq(), rework.getEntryToSeq(), rework.getReturnSeqNo(),
                rework.getMaxCount() == null ? 1 : rework.getMaxCount(),
                rework.getEnabled() != null && rework.getEnabled() == 1,
                first == null ? null : first.getSeqNo(),
                first == null ? null : first.getStepId());
    }
}
