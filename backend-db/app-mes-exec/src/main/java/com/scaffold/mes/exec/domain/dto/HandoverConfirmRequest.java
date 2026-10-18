package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * HandoverConfirmRequest 确认交接班
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class HandoverConfirmRequest {

    /** 接班人。 */
    @NotBlank
    private String toUser;
}
