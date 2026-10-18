package com.scaffold.mes.exec.domain.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * WoMaterialRequirement 工单物料需求
 * <p>
 * 对应 mes_wo_material_requirement。按批、按次、按时间的需求数量为空。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_wo_material_requirement")
public class WoMaterialRequirement extends NonSoftDeletableEntity {

    /** 工单。 */
    private Long woId;

    /** BOM 行。 */
    private Long bomItemId;

    /** 物料。 */
    private Long materialId;

    /** 消耗方式。 */
    private String consumeMode;

    /** 需求数量。无法推算时为空。 */
    private BigDecimal requiredQty;
}
