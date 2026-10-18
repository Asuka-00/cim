package com.scaffold.mes.warehouse.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * SupplierCreateRequest 新建供应商
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class SupplierCreateRequest {

    /** 编码。 */
    @NotBlank
    private String supplierCode;

    /** 名称。 */
    @NotBlank
    private String supplierName;

    /** 联系人。 */
    private String contact;

    /** 电话。 */
    private String phone;
}
