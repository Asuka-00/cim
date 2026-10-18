package com.scaffold.mes.base.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * EquipmentFaultCloseRequest 关闭设备异常
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class EquipmentFaultCloseRequest {

    /** 处理结果。 */
    @NotBlank
    private String solution;
}
