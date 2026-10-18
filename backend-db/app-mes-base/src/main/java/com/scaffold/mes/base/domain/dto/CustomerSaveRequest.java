package com.scaffold.mes.base.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * CustomerSaveRequest 客户保存请求
 * <p>
 * 新增与修改共用。未传启用状态时按启用处理。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class CustomerSaveRequest {

    /** 客户编码，租户内唯一。 */
    @NotBlank
    private String customerCode;

    /** 客户名称。 */
    @NotBlank
    private String customerName;

    /** 联系人。 */
    private String contact;

    /** 电话。 */
    private String phone;

    /** 备注。 */
    private String remark;

    /** 1 启用，0 停用。空则按启用处理。 */
    private Integer enabled;
}
