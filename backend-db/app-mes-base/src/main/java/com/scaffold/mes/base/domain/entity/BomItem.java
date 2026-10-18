package com.scaffold.mes.base.domain.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * BomItem BOM 行
 * <p>
 * 对应 mes_bom_item。routeSeqNo 为 0 表示该站点每次经过都消耗。损耗率只用于展开，不参与扣减。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_bom_item")
public class BomItem extends NonSoftDeletableEntity {

    /** 所属 BOM 版本。 */
    private Long bomVersionId;

    /** 行号，版本内唯一。 */
    private Integer lineNo;

    /** 物料。 */
    private Long materialId;

    /** 单位用量。 */
    private BigDecimal qtyPer;

    /** 用量单位。 */
    private Long uomId;

    /** 允许损耗率，百分比。 */
    private BigDecimal scrapRate;

    /** 消耗方式。 */
    private String consumeMode;

    /** 消耗站点。 */
    private Long stepId;

    /** 主流程步骤序号。0 表示该站点每次经过都消耗。 */
    private Integer routeSeqNo;

    /** 1 表示 Track-Out 自动反冲。 */
    private Integer backflush;

    /** 覆盖物料的关键料标识。空表示沿用物料。 */
    private Integer isCritical;

    /** 替代组。同组互为替代。 */
    private String substituteGroup;

    /** 组内优先级，数小优先。 */
    private Integer substitutePriority;

    /** 非首选替代策略：ALLOW、WARN、BLOCK。 */
    private String substitutePolicy;

    /** 备注。 */
    private String remark;
}
