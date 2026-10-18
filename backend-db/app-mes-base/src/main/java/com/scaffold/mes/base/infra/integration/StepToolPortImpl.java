package com.scaffold.mes.base.infra.integration;

import org.springframework.stereotype.Component;

import com.scaffold.mes.base.domain.service.StepToolApplicationService;
import com.scaffold.share.integration.mes.base.provider.StepToolPort;

import lombok.RequiredArgsConstructor;

/**
 * StepToolPortImpl 站点工具要求
 * <p>
 * 转给要求服务判断。没有要求时允许。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
public class StepToolPortImpl implements StepToolPort {

    private final StepToolApplicationService requirements;

    @Override
    public boolean accepted(Long stepId, Long productId, Long toolId, Long routeVersionId, Integer routeSeqNo) {
        return requirements.accepted(stepId, productId, toolId, routeVersionId, routeSeqNo);
    }
}
