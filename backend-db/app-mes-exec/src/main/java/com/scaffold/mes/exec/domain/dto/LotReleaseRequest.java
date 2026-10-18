package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * LotReleaseRequest 释放一条 Hold
 * <p>
 * 只释放指定的那一条。其他 ACTIVE 记录保持，hold_count 减 1。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class LotReleaseRequest {

    /** Hold 记录。 */
    @NotNull
    private Long holdId;

    /** 备注。 */
    private String remark;
}
