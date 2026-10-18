package com.scaffold.mes.base.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * AreaSaveRequest 区域保存请求
 * <p>
 * 新增与修改共用。未传启用状态时按启用处理。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class AreaSaveRequest {

    /** 区域编码，租户内唯一。 */
    @NotBlank
    private String areaCode;

    /** 区域名称。 */
    @NotBlank
    private String areaName;

    /** 上级区域，空表示顶层。 */
    private Long parentId;

    /** 备注。 */
    private String remark;

    /** 1 启用，0 停用。空则按启用处理。 */
    private Integer enabled;
}
