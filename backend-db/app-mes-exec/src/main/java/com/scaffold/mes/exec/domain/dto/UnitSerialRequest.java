package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * UnitSerialRequest 登记 Unit 序列号
 * <p>
 * 产品未打开跟踪时拒绝。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class UnitSerialRequest {

    /** 序列号。 */
    @NotBlank
    private String serialNo;

    /** Strip，可空。 */
    private Long stripId;

    /** 容器，可空。 */
    private Long containerId;

    /** 位置，可空。 */
    private String position;
}
