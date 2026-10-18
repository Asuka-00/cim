package com.scaffold.mes.base.domain.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * StepEquipmentGroupSaveRequest 站点设备组绑定保存请求
 * <p>
 * 新增与修改共用。未传优先级时按 0。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class StepEquipmentGroupSaveRequest {

    /** 站点。 */
    @NotNull
    private Long stepId;

    /** 设备组。 */
    @NotNull
    private Long groupId;

    /** 优先级，数小优先。空则按 0。 */
    private Integer priority;
}
