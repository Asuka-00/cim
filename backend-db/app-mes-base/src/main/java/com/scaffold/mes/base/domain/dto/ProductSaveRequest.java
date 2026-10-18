package com.scaffold.mes.base.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * ProductSaveRequest 产品保存请求
 * <p>
 * 新增与修改共用。未传产品类型时按 WAFER，未传启用状态时按启用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class ProductSaveRequest {

    /** 产品料号，租户内唯一。 */
    @NotBlank
    private String productCode;

    /** 产品名称。 */
    @NotBlank
    private String productName;

    /** 类型：WAFER、DIE、PACKAGE、OTHER。空则按 WAFER。 */
    private String productType;

    /** 产品族，可空。 */
    private Long familyId;

    /** 客户，可空。 */
    private Long customerId;

    /** 客户料号。 */
    private String customerPartNo;

    /** 晶圆尺寸，毫米。有值时只能是 150、200、300。 */
    private Integer waferSizeMm;

    /** 每片晶圆的毛 Die 数。 */
    private Integer grossDiePerWafer;

    /** 开批默认计数单位。 */
    @NotNull
    private Long baseUnitId;

    /** 备注。 */
    private String remark;

    /** 1 启用，0 停用。空则按启用处理。 */
    private Integer enabled;
}
