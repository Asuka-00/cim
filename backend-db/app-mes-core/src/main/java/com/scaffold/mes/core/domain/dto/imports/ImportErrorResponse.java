package com.scaffold.mes.core.domain.dto.imports;

/**
 * ImportErrorResponse 一条导入失败行
 *
 * @param id 主键
 * @param taskId 所属任务
 * @param rowNo 行号
 * @param message 错误信息
 * @param errorCode 错误码
 * @param rawData 原始行内容
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record ImportErrorResponse(Long id, Long taskId, Integer rowNo, String message, String errorCode,
        String rawData) {
}
