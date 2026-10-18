package com.scaffold.mes.exec.domain.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * TrackOutRequest 出站请求
 * <p>
 * M3 按 Route 序号进入下一步。报废选片在报废接口里做。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class TrackOutRequest {

    /** 备注。 */
    private String remark;

    /** 转换站点的实际数量。普通站点留空。 */
    private Long qtyAfter;

    /** 转换偏差超过允许范围时的确认原因。 */
    private String confirmReason;

    /** 本次使用的工具。空表示不计数。 */
    private Long toolId;
}
