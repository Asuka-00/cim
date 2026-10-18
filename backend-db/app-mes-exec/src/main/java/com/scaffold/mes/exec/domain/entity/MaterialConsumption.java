package com.scaffold.mes.exec.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.AppendOnlyEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * MaterialConsumption 物料消耗
 * <p>
 * 对应 hist_mes_material_consumption。只追加。出站反冲记为 BACKFLUSH。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("hist_mes_material_consumption")
public class MaterialConsumption extends AppendOnlyEntity {

    /** Lot。 */
    private Long lotId;

    /** 站点。 */
    private Long stepId;

    /** 物料。 */
    private Long materialId;

    /** 物料批。 */
    private Long materialLotId;

    /** 被扣减的库位。0 表示从设备绑定扣。 */
    private Long locationId;

    /** 设备绑定。0 表示直耗线边。 */
    private Long bindingId;

    /** 消耗数量。 */
    private BigDecimal qty;

    /** 计量单位。 */
    private Long uomId;

    /** 消耗方式。 */
    private String consumeMode;

    /** BACKFLUSH。 */
    private String source;

    /** 1 表示欠账，没有扣到库存。 */
    private Integer isShortage;

    /** 被清偿的欠账。 */
    private Long adjustRefId;

    /** 发生时间。 */
    private LocalDateTime occurredAt;
}
