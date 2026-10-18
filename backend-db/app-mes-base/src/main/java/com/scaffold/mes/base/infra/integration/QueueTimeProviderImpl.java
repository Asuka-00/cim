package com.scaffold.mes.base.infra.integration;

import org.springframework.stereotype.Component;

import com.scaffold.mes.base.domain.entity.RouteStep;
import com.scaffold.mes.base.domain.repository.RouteStepRepository;
import com.scaffold.share.integration.mes.base.provider.QueueTimeProvider;
import com.scaffold.share.integration.mes.base.response.QueueTimeLimit;

import lombok.RequiredArgsConstructor;

/**
 * QueueTimeProviderImpl 排队时间
 * <p>
 * 按 Route 版本和步骤序号读取上限。没有步骤时上限为空。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
public class QueueTimeProviderImpl implements QueueTimeProvider {

    private final RouteStepRepository repository;

    @Override
    public QueueTimeLimit limit(Long routeVersionId, Integer seqNo) {
        if (routeVersionId == null || seqNo == null) {
            return new QueueTimeLimit(null, null);
        }
        return repository.listByVersion(routeVersionId).stream()
                .filter(step -> seqNo.equals(step.getSeqNo()))
                .findFirst()
                .map(this::toLimit)
                .orElseGet(() -> new QueueTimeLimit(null, null));
    }

    private QueueTimeLimit toLimit(RouteStep step) {
        return new QueueTimeLimit(step.getQueueTimeMaxMin(), step.getQueueTimeAction());
    }
}
