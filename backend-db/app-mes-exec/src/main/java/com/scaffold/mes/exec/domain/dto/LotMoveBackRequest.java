package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * LotMoveBackRequest 退回上一步
 * <p>
 * 回到前一个步骤重走。已经记过的经过次数不回减，重走得到新的经过序号。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class LotMoveBackRequest {

    /** 原因码。 */
    @NotBlank
    private String reasonCode;

    /** 备注。 */
    private String remark;
}
