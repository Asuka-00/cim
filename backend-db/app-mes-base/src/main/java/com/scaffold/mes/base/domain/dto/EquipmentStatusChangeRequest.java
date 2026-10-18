package com.scaffold.mes.base.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * EquipmentStatusChangeRequest 设备状态切换请求
 * <p>
 * 手工切换的来源固定为 MANUAL。停机类状态必须带原因码。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class EquipmentStatusChangeRequest {

    /** 目标 E10 大类。 */
    @NotBlank
    private String e10State;

    /** 细分状态码。 */
    private String statusCode;

    /** 原因码。停机类必填。 */
    private String reasonCode;

    /** 备注。 */
    private String remark;
}
