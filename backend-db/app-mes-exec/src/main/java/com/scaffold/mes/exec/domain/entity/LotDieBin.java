package com.scaffold.mes.exec.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * LotDieBin Die Bin 数量
 * <p>
 * 对应 mes_lot_die_bin。waferId 为 0 是 Lot 汇总行，非 0 是逐片行。两种不能同时存在。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_lot_die_bin")
public class LotDieBin extends NonSoftDeletableEntity {

    /** Lot。 */
    private Long lotId;

    /** Wafer。0 表示汇总行。 */
    private Long waferId;

    /** Bin 编码。 */
    private String binCode;

    /** Bin 名称。 */
    private String binName;

    /** 1 表示良品 Bin。 */
    private Integer isGood;

    /** Die 数量。 */
    private Long dieQty;

    /** MANUAL 或 IMPORT。 */
    private String source;
}
