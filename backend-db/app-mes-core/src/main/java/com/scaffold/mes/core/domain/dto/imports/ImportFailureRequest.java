package com.scaffold.mes.core.domain.dto.imports;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * ImportFailureRequest 记一条导入失败行
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class ImportFailureRequest {

    /** 行号，从 1 开始。 */
    @NotNull
    private Integer rowNo;

    /** 错误信息。 */
    @NotBlank
    private String message;

    /** 错误码。空则不记。 */
    private String errorCode;

    /** 原始行内容。空则不记。 */
    private String rawData;
}
