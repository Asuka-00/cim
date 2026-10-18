package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * LotRouteChangeRequest 改 Route
 * <p>
 * 新产品版本可以不传，表示产品版本不变。目标步骤必须在新 Route 上。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class LotRouteChangeRequest {

    /** 新 Route 版本。 */
    @NotNull
    private Long routeVersionId;

    /** 新产品版本。空则保持原值。 */
    private Long productVersionId;

    /** 新 Route 上的步骤序号。 */
    @NotNull
    private Integer routeSeqNo;

    /** 原因码。 */
    @NotBlank
    private String reasonCode;
}
