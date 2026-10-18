package com.scaffold.mes.core.domain.dto.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * MesFileRegisterRequest 登记挂在 Lot 上的文件
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class MesFileRegisterRequest {

    /** 原始文件名。 */
    @NotBlank
    private String fileName;

    /** MIME 类型。 */
    private String contentType;

    /** 文件大小，字节。 */
    @NotNull
    private Long sizeBytes;

    /** 内容摘要，64 位十六进制。 */
    @NotBlank
    private String sha256;

    /** Lot。 */
    @NotNull
    private Long lotId;
}
