package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * HandoverCreateRequest 新建交接班
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class HandoverCreateRequest {

    /** 交接单号。 */
    @NotBlank
    private String handoverNo;

    /** 交班人。 */
    @NotBlank
    private String fromUser;

    /** 交班备注。 */
    private String remark;
}
