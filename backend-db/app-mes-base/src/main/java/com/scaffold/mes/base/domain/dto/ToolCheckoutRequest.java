package com.scaffold.mes.base.domain.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * ToolCheckoutRequest 工具领用
 * <p>
 * 把可用工具装到指定设备。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class ToolCheckoutRequest {

    /** 设备。 */
    @NotNull
    private Long equipmentId;
}
