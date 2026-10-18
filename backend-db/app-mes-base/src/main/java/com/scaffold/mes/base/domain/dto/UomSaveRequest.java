package com.scaffold.mes.base.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * UomSaveRequest 物料计量单位保存请求
 * <p>
 * 新增与修改共用。未传小数位时按 0，未传启用状态时按启用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class UomSaveRequest {

    /** 单位编码，租户内唯一。 */
    @NotBlank
    private String uomCode;

    /** 单位名称。 */
    @NotBlank
    private String uomName;

    /** 量纲：COUNT、VOLUME、WEIGHT、LENGTH、AREA、TIME、OTHER。 */
    @NotBlank
    private String dimension;

    /** 默认小数位数。空则按 0。 */
    private Integer decimals;

    /** 1 启用，0 停用。空则按启用处理。 */
    private Integer enabled;
}
