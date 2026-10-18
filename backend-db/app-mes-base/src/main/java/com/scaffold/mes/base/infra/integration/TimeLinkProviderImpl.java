package com.scaffold.mes.base.infra.integration;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.scaffold.mes.base.domain.repository.RouteTimeLinkRepository;
import com.scaffold.share.integration.mes.base.provider.TimeLinkProvider;
import com.scaffold.share.integration.mes.base.response.TimeLinkStart;

import lombok.RequiredArgsConstructor;

/**
 * TimeLinkProviderImpl 时间链
 * <p>
 * 从 Route 版本里筛出起点序号匹配的约束。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
public class TimeLinkProviderImpl implements TimeLinkProvider {

    private final RouteTimeLinkRepository repository;

    @Override
    public List<TimeLinkStart> startingAt(Long routeVersionId, Integer fromSeqNo) {
        if (routeVersionId == null || fromSeqNo == null) {
            return List.of();
        }
        return repository.listByVersion(routeVersionId).stream()
                .filter(link -> fromSeqNo.equals(link.getFromSeqNo()))
                .map(link -> new TimeLinkStart(link.getId(), link.getFromSeqNo(), link.getToSeqNo(), link.getMaxMinutes(),
                        link.getAction(), link.getReasonCode()))
                .toList();
    }

    @Override
    public Optional<TimeLinkStart> find(Long routeVersionId, Long timeLinkId) {
        if (routeVersionId == null || timeLinkId == null) {
            return Optional.empty();
        }
        return repository.listByVersion(routeVersionId).stream()
                .filter(link -> timeLinkId.equals(link.getId()))
                .map(link -> new TimeLinkStart(link.getId(), link.getFromSeqNo(), link.getToSeqNo(), link.getMaxMinutes(),
                        link.getAction(), link.getReasonCode()))
                .findFirst();
    }
}
