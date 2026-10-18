package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * WaferReenterRequest 回收片再入厂
 * <p>
 * 生成新的内部编号，指向原来的片。客户编号保持不变。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class WaferReenterRequest {

    /** 已回收的原片。 */
    @NotNull
    private Long reclaimedWaferId;

    /** 新的本厂编号。 */
    @NotBlank
    private String waferCode;
}
