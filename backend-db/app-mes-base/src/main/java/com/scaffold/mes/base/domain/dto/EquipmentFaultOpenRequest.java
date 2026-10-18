package com.scaffold.mes.base.domain.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * EquipmentFaultOpenRequest 登记设备异常
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class EquipmentFaultOpenRequest {

    /** 单号。 */
    @NotBlank
    private String faultNo;

    /** 设备。 */
    @NotNull
    private Long equipmentId;

    /** 描述。 */
    @NotBlank
    private String description;

    /** MINOR、MAJOR 或 CRITICAL。空表示 MINOR。 */
    private String severity;

    /** 受影响的 Lot。 */
    private List<Long> lotIds;

    /** 为 true 时把设备切到非计划停机。 */
    private Boolean downEquipment;
}
