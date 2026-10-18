package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * LotUndoTrackOutRequest 撤销出站
 * <p>
 * 按最近一次出站前的快照回到加工中。原因必填。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class LotUndoTrackOutRequest {

    /** 原因码。 */
    @NotBlank
    private String reasonCode;

    /** 备注。 */
    private String remark;

    /** 出站时计过数的工具。空表示不回减。 */
    private Long toolId;
}
