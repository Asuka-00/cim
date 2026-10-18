package com.scaffold.mes.base.domain.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * RouteStepSaveRequest Route 步骤保存请求
 * <p>
 * 新增与修改共用。转换站点必须带转换后的计数单位。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class RouteStepSaveRequest {

    /** 步骤序号，版本内唯一。 */
    @NotNull
    private Integer seqNo;

    /** 站点。同一版本可以重复。 */
    @NotNull
    private Long stepId;

    /** 重入时的别名。 */
    private String stepAlias;

    /** 1 表示必经。空则按必经。 */
    private Integer isRequired;

    /** 步骤指定设备组。空表示用站点级绑定。 */
    private Long equipmentGroupId;

    /** 进入本步骤时的计数单位。 */
    private Long countUnitId;

    /** 1 表示单位转换站点。空则按否。 */
    private Integer isUnitConvert;

    /** 转换后的计数单位。 */
    private Long convertToUnitId;

    /** 标准加工时长，分钟。 */
    private BigDecimal stdProcessMin;

    /** 进站到 Track-In 的最长等待，分钟。 */
    private Integer queueTimeMaxMin;

    /** Queue Time 预警阈值，分钟。 */
    private Integer queueTimeWarnMin;

    /** 超时动作：HOLD 或 WARN。 */
    private String queueTimeAction;
}
