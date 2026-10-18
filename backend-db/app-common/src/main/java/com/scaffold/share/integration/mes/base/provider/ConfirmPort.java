package com.scaffold.share.integration.mes.base.provider;

/**
 * ConfirmPort 二次确认
 * <p>
 * 执行模块不能直接读规则和签名。没有规则时允许。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface ConfirmPort {

    /**
     * 这个动作对这个对象是否已经确认。
     *
     * @param actionCode 动作
     * @param bizType 对象类型
     * @param bizId 对象
     * @return 不需要确认，或已有对应签名时为 true
     */
    boolean allowed(String actionCode, String bizType, Long bizId);
}
