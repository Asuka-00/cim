package com.scaffold.mes.exec.domain.service.interlock;

/**
 * InterlockHit 一条规则的结果
 *
 * @param rule 规则编码
 * @param severity BLOCK、WARN 或 SKIP
 * @param message 给人看的原因
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record InterlockHit(String rule, String severity, String message) {

    /**
     * 这条规则不检查。
     *
     * @param rule 规则编码
     * @return SKIP
     */
    public static InterlockHit skip(String rule) {
        return new InterlockHit(rule, "SKIP", "");
    }

    /**
     * 阻断进站。
     *
     * @param rule 规则编码
     * @param message 原因
     * @return BLOCK
     */
    public static InterlockHit block(String rule, String message) {
        return new InterlockHit(rule, "BLOCK", message);
    }

    /**
     * 通过。
     *
     * @param rule 规则编码
     * @return 空消息的通过结果，严重度用 PASS 表示，不阻断
     */
    public static InterlockHit pass(String rule) {
        return new InterlockHit(rule, "PASS", "");
    }
}
