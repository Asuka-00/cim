package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * PackContainerCreateRequest 新建包装容器
 * <p>
 * 空容器挂到 Lot 上，状态为 OPEN。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class PackContainerCreateRequest {

    /** 容器号。 */
    @NotBlank
    private String containerNo;

    /** TRAY、REEL、TUBE 或 BOX。 */
    @NotBlank
    private String containerType;

    /** 容量，至少 1。 */
    @NotNull
    private Integer capacity;
}
