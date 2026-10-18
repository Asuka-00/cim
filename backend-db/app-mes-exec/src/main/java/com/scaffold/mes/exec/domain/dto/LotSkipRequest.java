package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * LotSkipRequest 跳到下一步
 * <p>
 * 只跳一个序号。被跳过的站点不增加经过次数。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class LotSkipRequest {

    /** 原因码。 */
    @NotBlank
    private String reasonCode;

    /** 备注。 */
    private String remark;
}
