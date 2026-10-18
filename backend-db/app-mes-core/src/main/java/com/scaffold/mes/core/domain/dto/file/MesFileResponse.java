package com.scaffold.mes.core.domain.dto.file;

/**
 * MesFileResponse 挂在 Lot 上的文件
 *
 * @param id 文件
 * @param fileName 原始文件名
 * @param sizeBytes 大小，字节
 * @param sha256 内容摘要
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record MesFileResponse(Long id, String fileName, Long sizeBytes, String sha256) {
}
