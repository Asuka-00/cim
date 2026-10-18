package com.scaffold.mes.core.domain.entity.sequence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * SequenceRule 编号规则
 * <p>
 * 对应 mes_sequence_rule。流水按租户与周期重置。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_sequence_rule")
public class SequenceRule extends NonSoftDeletableEntity {

    /** 规则编码,如 WORK_ORDER/LOT/WAFER/BATCH/MATERIAL_LOT/STRIP/REEL */
    private String ruleCode;

    /** 规则名称 */
    private String ruleName;

    /** 业务对象类型 */
    private String bizType;

    /** 前缀 */
    private String prefix;

    /** 日期模板:yyyyMMdd/yyMM/yyMMdd/yyyy,空表示无 */
    private String datePattern;

    /** 流水号长度,不足左补 0 */
    private Integer seqLength;

    /** 流水起始值 */
    private Long seqStart;

    /** 流水步长 */
    private Integer seqStep;

    /** 重置周期 NEVER/DAY/MONTH/YEAR */
    private String resetCycle;

    /** 后缀 */
    private String suffix;

    /** 备注 */
    private String remark;

    /** 启用状态 0-禁用 1-启用 */
    private Integer enabled;

}
