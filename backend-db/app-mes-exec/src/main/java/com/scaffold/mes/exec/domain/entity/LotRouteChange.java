package com.scaffold.mes.exec.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.AppendOnlyEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * LotRouteChange 工艺变更记录
 * <p>
 * 对应 hist_mes_lot_route_change。只追加。记下改 Route 前后的版本和步骤。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("hist_mes_lot_route_change")
public class LotRouteChange extends AppendOnlyEntity {

    /** Lot。 */
    private Long lotId;

    /** 原产品版本。 */
    private Long fromProductVersionId;

    /** 新产品版本。 */
    private Long toProductVersionId;

    /** 原 Route 版本。 */
    private Long fromRouteVersionId;

    /** 新 Route 版本。 */
    private Long toRouteVersionId;

    /** 变更前步骤序号。 */
    private Integer fromSeqNo;

    /** 变更后步骤序号。 */
    private Integer toSeqNo;

    /** 原因码。 */
    private String reasonCode;

    /** 操作人。 */
    private String operator;

    /** 发生时间。 */
    private LocalDateTime occurredAt;
}
