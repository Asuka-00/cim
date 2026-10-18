package com.scaffold.mes.exec.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.AppendOnlyEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * LotRelation Lot 之间的一次拆合
 * <p>
 * 对应 hist_mes_lot_relation。只追加。显式拆批写 SPLIT。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("hist_mes_lot_relation")
public class LotRelation extends AppendOnlyEntity {

    /** 父 Lot 或源 Lot。 */
    private Long parentLotId;

    /** 子 Lot 或目标 Lot。 */
    private Long childLotId;

    /** 关系类型。 */
    private String relationType;

    /** 涉及数量。 */
    private Long qty;

    /** 计数单位。 */
    private Long unitId;

    /** 发生时间。 */
    private LocalDateTime occurredAt;

    /** 备注。 */
    private String remark;
}
