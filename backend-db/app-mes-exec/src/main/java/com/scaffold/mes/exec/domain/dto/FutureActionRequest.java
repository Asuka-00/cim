package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * FutureActionRequest 预约一个到站动作
 * <p>
 * 只在主流程步骤到达时触发。Hold 的参数是原因码，改优先级的参数是 1 到 9。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class FutureActionRequest {

    /** 触发的主流程序号。 */
    @NotNull
    private Integer routeSeqNo;

    /** 第几次经过。空表示每次。 */
    private Integer passNo;

    /** HOLD 或 CHANGE_PRIORITY。 */
    @NotBlank
    private String actionType;

    /** 动作参数。 */
    @NotBlank
    private String actionParams;
}
