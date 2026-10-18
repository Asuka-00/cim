package com.scaffold.mes.base.domain.service;

import org.springframework.stereotype.Service;

import com.scaffold.mes.base.domain.repository.EsignRepository;

import lombok.RequiredArgsConstructor;

/**
 * ConfirmGateApplicationService 二次确认门
 * <p>
 * 没有规则时直接通过。再次确认要有 PRIMARY 签名，第二人签名要有 SECOND 签名。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class ConfirmGateApplicationService {

    private final ConfirmRuleApplicationService rules;
    private final EsignRepository esigns;

    /**
     * 这个动作对这个对象是否已经确认。
     *
     * @param actionCode 动作
     * @param bizType 对象类型
     * @param bizId 对象
     * @return 不需要确认，或已有对应签名时为 true
     */
    public boolean allowed(String actionCode, String bizType, Long bizId) {
        String mode = rules.required(actionCode);
        if ("NONE".equals(mode)) {
            return true;
        }
        String signType = "SECOND_SIGN".equals(mode) ? "SECOND" : "PRIMARY";
        return esigns.exists(actionCode, bizType, bizId, signType);
    }
}
