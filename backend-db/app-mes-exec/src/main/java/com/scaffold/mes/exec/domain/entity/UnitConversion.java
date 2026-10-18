package com.scaffold.mes.exec.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.AppendOnlyEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * UnitConversion 单位转换历史
 * <p>
 * 对应 hist_mes_unit_conversion。只追加。转换不允许撤销。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("hist_mes_unit_conversion")
public class UnitConversion extends AppendOnlyEntity {

    /** Lot。 */
    private Long lotId;

    /** 事务。可空。 */
    private Long txnId;

    /** 转换步骤序号。 */
    private Integer routeSeqNo;

    /** 转换站点。 */
    private Long stepId;

    /** 转换前单位。 */
    private Long fromUnitId;

    /** 转换后单位。 */
    private Long toUnitId;

    /** 转换前数量。 */
    private Long qtyBefore;

    /** 按系数算出的参考数量。 */
    private Long qtyReference;

    /** 转换后实际数量。 */
    private Long qtyAfter;

    /** 参考数量减去实际数量。 */
    private Long lossQty;

    /** 偏差百分比。 */
    private BigDecimal deviationPct;

    /** 使用的系数。 */
    private BigDecimal factorUsed;

    /** 转换前的当前报废数量。 */
    private Long scrapArchivedQty;

    /** 超偏差时的确认原因。 */
    private String confirmReason;

    /** 操作人。 */
    private String operator;

    /** 发生时间。 */
    private LocalDateTime occurredAt;
}
