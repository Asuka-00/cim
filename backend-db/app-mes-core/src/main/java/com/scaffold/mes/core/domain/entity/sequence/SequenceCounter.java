package com.scaffold.mes.core.domain.entity.sequence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * SequenceCounter 编号流水计数
 * <p>
 * 对应 mes_sequence_counter。取号时对该行加锁后步进。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_sequence_counter")
public class SequenceCounter extends NonSoftDeletableEntity {

    /** 规则ID */
    private Long ruleId;

    /** 周期键:NEVER 时为 -,DAY 时为 yyyyMMdd,MONTH 为 yyyyMM,YEAR 为 yyyy */
    private String periodKey;

    /** 当前已发放的最大流水值 */
    private Long currentValue;

}
