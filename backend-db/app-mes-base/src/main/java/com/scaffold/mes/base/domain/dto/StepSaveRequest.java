package com.scaffold.mes.base.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * StepSaveRequest 站点保存请求
 * <p>
 * 新增与修改共用。未传类型时按 PROCESS。未传设备或 Recipe 标志时，目检、HOLD、SHIP、仓储为 0，其余为 1。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class StepSaveRequest {

    /** 站点编码，租户内唯一。 */
    @NotBlank
    private String stepCode;

    /** 站点名称。 */
    @NotBlank
    private String stepName;

    /** 类型。空则按 PROCESS。 */
    private String stepType;

    /** 所属区域，可空。 */
    private Long areaId;

    /** 所属站点组，可空。 */
    private Long stepGroupId;

    /** 是否需要设备。空则按站点类型默认。 */
    private Integer needEquipment;

    /** 是否需要 Recipe。空则按站点类型默认。 */
    private Integer needRecipe;

    /** 工艺说明。 */
    private String description;

    /** 1 启用，0 停用。空则按启用处理。 */
    private Integer enabled;
}
