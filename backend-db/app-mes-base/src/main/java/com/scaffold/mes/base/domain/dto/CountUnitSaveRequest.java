package com.scaffold.mes.base.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * CountUnitSaveRequest 计数单位保存请求
 * <p>
 * 新增与修改共用。未传启用状态时按启用处理。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class CountUnitSaveRequest {

    /** 单位编码，租户内唯一。 */
    @NotBlank
    private String unitCode;

    /** 单位名称。 */
    @NotBlank
    private String unitName;

    /** 类别：WAFER、DIE、STRIP、UNIT、CARRIER、OTHER。 */
    @NotBlank
    private String unitType;

    /** 1 表示最小可计数单位。 */
    private Integer isBase;

    /** 排序号。 */
    private Integer sortNo;

    /** 备注。 */
    private String remark;

    /** 1 启用，0 停用。空则按启用处理。 */
    private Integer enabled;
}
