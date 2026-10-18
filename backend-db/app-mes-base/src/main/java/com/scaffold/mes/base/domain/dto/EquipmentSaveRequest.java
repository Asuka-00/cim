package com.scaffold.mes.base.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * EquipmentSaveRequest 设备台账保存请求
 * <p>
 * 只改台账字段。当前状态不在这里改，避免绕过历史时间段。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class EquipmentSaveRequest {

    /** 设备编码，租户内唯一。 */
    @NotBlank
    private String equipmentCode;

    /** 设备名称。 */
    @NotBlank
    private String equipmentName;

    /** 类型。空则按 PROCESS。 */
    private String equipmentType;

    /** 设备组，可空。 */
    private Long groupId;

    /** 所属区域，可空。 */
    private Long areaId;

    /** 厂商。 */
    private String vendor;

    /** 型号。 */
    private String model;

    /** 序列号。 */
    private String serialNo;

    /** 备注。 */
    private String remark;

    /** 1 启用，0 停用。空则按启用处理。 */
    private Integer enabled;
}
