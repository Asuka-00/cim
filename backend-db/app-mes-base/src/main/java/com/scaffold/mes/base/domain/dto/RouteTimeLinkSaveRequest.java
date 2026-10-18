package com.scaffold.mes.base.domain.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * RouteTimeLinkSaveRequest Time Link 保存请求
 * <p>
 * 新增与修改共用。未传动作时按 HOLD，HOLD 必须带原因码。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class RouteTimeLinkSaveRequest {

    /** 起点步骤序号。 */
    @NotNull
    private Integer fromSeqNo;

    /** 终点步骤序号。 */
    @NotNull
    private Integer toSeqNo;

    /** 最长间隔，分钟。 */
    @NotNull
    private Integer maxMinutes;

    /** 预警阈值，分钟。空表示不预警。 */
    private Integer warnMinutes;

    /** 超时动作。空则按 HOLD。 */
    private String action;

    /** 自动 Hold 的原因码。 */
    private String reasonCode;
}
