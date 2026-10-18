package com.scaffold.mes.core.domain.dto.uda;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * UdaDefCreateRequest 新建自定义属性定义
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class UdaDefCreateRequest {

    /** 对象类型。 */
    @NotBlank
    private String objectType;

    /** 属性编码。 */
    @NotBlank
    private String attrCode;

    /** 属性名称。 */
    @NotBlank
    private String attrName;

    /** 数据类型。空则 TEXT。 */
    private String dataType;

    /** 枚举选项，逗号分隔。ENUM 时必填。 */
    private String options;

    /** 1 表示必填。空则不必填。 */
    private Integer required;
}
