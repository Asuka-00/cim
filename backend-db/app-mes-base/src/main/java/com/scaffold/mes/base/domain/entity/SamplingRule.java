package com.scaffold.mes.base.domain.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * SamplingRule 抽检规则
 * <p>
 * 对应 mes_sampling_rule。EVERY_N_LOT 表示每 N 个 Lot 抽 1 个。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_sampling_rule")
public class SamplingRule extends NonSoftDeletableEntity {

    /** 规则编码。 */
    private String ruleCode;

    /** 规则名称。 */
    private String ruleName;

    /** EVERY_N_LOT 或 PERCENT。 */
    private String sampleType;

    /** 每 N 个 Lot 抽 1 个。 */
    private Integer nValue;

    /** 抽检百分比。 */
    private BigDecimal percent;

    /** 每 Lot 抽取片数。空表示全部。 */
    private Integer waferCount;

    /** 指定槽位，逗号分隔。 */
    private String slotList;

    /** 1 表示启用。 */
    private Integer enabled;
}
