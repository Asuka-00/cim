package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * WaferMapImportRequest 导入 Wafer Map
 * <p>
 * 每行格式为 binCode,dieQty,good。good 为 1 表示良品。表头行会被跳过。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class WaferMapImportRequest {

    /** Wafer。 */
    @NotNull
    private Long waferId;

    /** 源文件名。 */
    private String fileName;

    /** CSV 文本。 */
    @NotBlank
    private String csv;
}
