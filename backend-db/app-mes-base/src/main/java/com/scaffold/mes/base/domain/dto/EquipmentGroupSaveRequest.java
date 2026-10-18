package com.scaffold.mes.base.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * EquipmentGroupSaveRequest 设备组保存请求
 * <p>
 * 新增与修改共用。未传启用状态时按启用处理。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class EquipmentGroupSaveRequest {

    /** 设备组编码，租户内唯一。 */
    @NotBlank
    private String groupCode;

    /** 设备组名称。 */
    @NotBlank
    private String groupName;

    /** 所属区域，可空。 */
    private Long areaId;

    /** 备注。 */
    private String remark;

    /** 1 启用，0 停用。空则按启用处理。 */
    private Integer enabled;
}
