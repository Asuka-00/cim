package com.scaffold.mes.core.domain.dto.imports;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * ImportTaskCreateRequest 新建导入任务
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class ImportTaskCreateRequest {

    /** 任务号。 */
    @NotBlank
    private String taskNo;

    /** 导入类型。 */
    @NotBlank
    private String importType;

    /** VALIDATE_ONLY 或 COMMIT。空则按 COMMIT。 */
    private String mode;

    /** SKIP 或 ROLLBACK。空则按 SKIP。 */
    private String onError;

    /** 文件引用。空则不记。 */
    private String fileRef;

    /** 操作人。空则不记。 */
    private String operator;
}
