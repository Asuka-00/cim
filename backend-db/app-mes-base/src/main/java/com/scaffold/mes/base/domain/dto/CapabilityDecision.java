package com.scaffold.mes.base.domain.dto;

/**
 * CapabilityDecision 设备能力判定
 * <p>
 * allowed 为 false 时，code 说明原因：INHIBIT、OUT_OF_WINDOW、NO_MATCH、NO_CAPABILITY。
 *
 * @param allowed 是否放行
 * @param code 判定码。放行时为 MATCH 或 GROUP
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record CapabilityDecision(boolean allowed, String code) {
}
