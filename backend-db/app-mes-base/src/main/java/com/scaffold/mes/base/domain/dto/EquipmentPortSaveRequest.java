package com.scaffold.mes.base.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * EquipmentPortSaveRequest 设备端口保存请求
 * <p>
 * 只改台账字段。当前 E10 不在这里改。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class EquipmentPortSaveRequest {

    /** 端口编码，设备内唯一。 */
    @NotBlank
    private String portCode;

    /** 名称。 */
    private String portName;

    /** 类型：PORT 或 CHAMBER。空则按 PORT。 */
    private String kind;

    /** 同时加工上限。空则按 1。 */
    private Integer maxConcurrent;

    /** 1 启用，0 停用。空则按启用。 */
    private Integer enabled;
}
