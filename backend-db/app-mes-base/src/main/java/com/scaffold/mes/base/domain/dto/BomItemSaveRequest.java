package com.scaffold.mes.base.domain.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * BomItemSaveRequest BOM 行保存请求
 * <p>
 * 新增与修改共用。未传消耗方式时按 PER_WAFER，未传步骤序号时按 0。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class BomItemSaveRequest {

    /** 行号，版本内唯一。 */
    @NotNull
    private Integer lineNo;

    /** 物料。 */
    @NotNull
    private Long materialId;

    /** 单位用量，必须大于 0。 */
    @NotNull
    private BigDecimal qtyPer;

    /** 用量单位。 */
    @NotNull
    private Long uomId;

    /** 允许损耗率。空则按 0。 */
    private BigDecimal scrapRate;

    /** 消耗方式。空则按 PER_WAFER。 */
    private String consumeMode;

    /** 消耗站点。 */
    @NotNull
    private Long stepId;

    /** 主流程步骤序号。空则按 0。 */
    private Integer routeSeqNo;

    /** 1 表示自动反冲。空则按是。 */
    private Integer backflush;

    /** 覆盖关键料。空表示沿用物料。 */
    private Integer isCritical;

    /** 替代组。 */
    private String substituteGroup;

    /** 组内优先级。 */
    private Integer substitutePriority;

    /** 非首选替代策略。空则按 ALLOW。 */
    private String substitutePolicy;

    /** 备注。 */
    private String remark;
}
