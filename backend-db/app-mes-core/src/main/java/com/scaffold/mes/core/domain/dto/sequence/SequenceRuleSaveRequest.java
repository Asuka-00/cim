package com.scaffold.mes.core.domain.dto.sequence;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * SequenceRuleSaveRequest 编号规则保存请求
 * <p>
 * 新增与修改共用。未传启用状态时按启用处理。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class SequenceRuleSaveRequest {

    /** 规则编码,租户内唯一。 */
    @NotBlank
    private String ruleCode;

    /** 规则名称。 */
    @NotBlank
    private String ruleName;

    /** 业务对象类型,如 WORK_ORDER、LOT。 */
    @NotBlank
    private String bizType;

    /** 前缀,可空。 */
    private String prefix;

    /** 日期模板,如 yyyyMMdd、yyMM。空表示不含日期。 */
    private String datePattern;

    /** 流水位数,不足左补 0。 */
    @NotNull
    private Integer seqLength;

    /** 流水起始值。 */
    @NotNull
    private Long seqStart;

    /** 流水步长。 */
    @NotNull
    private Integer seqStep;

    /** 重置周期:NEVER、DAY、MONTH 或 YEAR。 */
    @NotBlank
    private String resetCycle;

    /** 后缀,可空。 */
    private String suffix;

    /** 备注,可空。 */
    private String remark;

    /** 1 启用,0 停用。空则按启用处理。 */
    private Integer enabled;
}
