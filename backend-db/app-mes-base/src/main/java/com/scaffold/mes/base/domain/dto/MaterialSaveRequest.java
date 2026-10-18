package com.scaffold.mes.base.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * MaterialSaveRequest 物料保存请求
 * <p>
 * 新增与修改共用。未传类型时按 RAW，未传关键料和来料检验时按否。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class MaterialSaveRequest {

    /** 物料编码，租户内唯一。 */
    @NotBlank
    private String materialCode;

    /** 物料名称。 */
    @NotBlank
    private String materialName;

    /** 类型。空则按 RAW。 */
    private String materialType;

    /** 规格型号。 */
    private String spec;

    /** 基础计量单位。 */
    @NotNull
    private Long baseUomId;

    /** 1 表示关键料。空则按否。 */
    private Integer isCritical;

    /** 保质期天数。空表示不限。 */
    private Integer shelfLifeDays;

    /** 1 表示来料要检验。空则按否。 */
    private Integer needIncomingInspect;

    /** 备注。 */
    private String remark;

    /** 1 启用，0 停用。空则按启用处理。 */
    private Integer enabled;
}
