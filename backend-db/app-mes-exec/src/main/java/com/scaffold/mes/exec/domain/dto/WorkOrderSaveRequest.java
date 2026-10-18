package com.scaffold.mes.exec.domain.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * WorkOrderSaveRequest 创建工单请求
 * <p>
 * 只指定产品版本和计划量。Route、BOM 的具体版本由服务在下单时固化，调用方不能指定。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class WorkOrderSaveRequest {

    /** 工单号。 */
    @NotBlank
    private String woNo;

    /** 生效的产品版本。 */
    @NotNull
    private Long productVersionId;

    /** 计划数量，必须大于 0。 */
    @NotNull
    private Long planQty;

    /** 起始计数单位。 */
    @NotNull
    private Long qtyUnitId;

    /** 优先级。空则按 5。 */
    private Integer priority;

    /** 交期。 */
    private LocalDate dueDate;

    /** 备注。 */
    private String remark;
}
