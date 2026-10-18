package com.scaffold.mes.base.infra.integration;

import java.util.Comparator;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.scaffold.mes.base.domain.entity.RouteStep;
import com.scaffold.mes.base.domain.entity.Step;
import com.scaffold.mes.base.domain.repository.RouteStepRepository;
import com.scaffold.mes.base.domain.repository.StepRepository;
import com.scaffold.share.integration.mes.base.provider.RouteNextProvider;
import com.scaffold.share.integration.mes.base.response.RouteStepNext;

import lombok.RequiredArgsConstructor;

/**
 * RouteNextProviderImpl 默认下一步
 * <p>
 * 在同一 Route 版本里取比当前序号更大的最小步骤，并带上站点区域。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
public class RouteNextProviderImpl implements RouteNextProvider {

    private final RouteStepRepository routeStepRepository;
    private final StepRepository stepRepository;

    @Override
    public Optional<RouteStepNext> next(Long routeVersionId, Integer currentSeq) {
        int current = currentSeq == null ? 0 : currentSeq;
        return routeStepRepository.listByVersion(routeVersionId).stream()
                .filter(step -> step.getSeqNo() != null && step.getSeqNo() > current)
                .min(Comparator.comparing(RouteStep::getSeqNo))
                .map(step -> new RouteStepNext(step.getSeqNo(), step.getStepId(), areaOf(step.getStepId())));
    }

    @Override
    public Optional<RouteStepNext> at(Long routeVersionId, Integer seqNo) {
        if (seqNo == null) {
            return Optional.empty();
        }
        return routeStepRepository.listByVersion(routeVersionId).stream()
                .filter(step -> seqNo.equals(step.getSeqNo()))
                .findFirst()
                .map(step -> new RouteStepNext(step.getSeqNo(), step.getStepId(), areaOf(step.getStepId())));
    }

    @Override
    public Optional<RouteStepNext> previous(Long routeVersionId, Integer currentSeq) {
        int current = currentSeq == null ? 0 : currentSeq;
        return routeStepRepository.listByVersion(routeVersionId).stream()
                .filter(step -> step.getSeqNo() != null && step.getSeqNo() < current)
                .max(Comparator.comparing(RouteStep::getSeqNo))
                .map(step -> new RouteStepNext(step.getSeqNo(), step.getStepId(), areaOf(step.getStepId())));
    }

    @Override
    public boolean sampling(Long routeVersionId, Integer seqNo) {
        if (seqNo == null) {
            return false;
        }
        return routeStepRepository.listByVersion(routeVersionId).stream()
                .filter(step -> seqNo.equals(step.getSeqNo()))
                .findFirst()
                .map(step -> Integer.valueOf(1).equals(step.getIsSamplingStep()))
                .orElse(false);
    }

    private Long areaOf(Long stepId) {
        return stepRepository.findById(stepId).map(Step::getAreaId).orElse(null);
    }
}
