package com.scaffold.mes.core.domain.dto.print;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * PrintTemplateCreateRequest 新建打印模板
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class PrintTemplateCreateRequest {

    /** 模板编码。 */
    @NotBlank
    private String tplCode;

    /** 模板名称。 */
    @NotBlank
    private String tplName;

    /** 模板类型。 */
    @NotBlank
    private String tplType;

    /** 模板内容。 */
    @NotBlank
    private String content;
}
