package com.scaffold.mes.base.infra.integration;

import org.springframework.stereotype.Component;

import com.scaffold.mes.base.domain.service.ConfirmGateApplicationService;
import com.scaffold.share.integration.mes.base.provider.ConfirmPort;

import lombok.RequiredArgsConstructor;

/**
 * ConfirmPortImpl 二次确认
 * <p>
 * 转给确认门判断。没有规则时允许。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
public class ConfirmPortImpl implements ConfirmPort {

    private final ConfirmGateApplicationService gate;

    @Override
    public boolean allowed(String actionCode, String bizType, Long bizId) {
        return gate.allowed(actionCode, bizType, bizId);
    }
}
