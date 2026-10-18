package com.scaffold.mes.core.domain.dto.imports;

import java.time.LocalDateTime;

/**
 * ImportTaskResponse 一条导入任务
 *
 * @param id 主键
 * @param taskNo 任务号
 * @param importType 导入类型
 * @param mode 模式
 * @param onError 出错策略
 * @param status 状态
 * @param totalRows 总行数
 * @param successRows 成功行数
 * @param failRows 失败行数
 * @param fileRef 文件引用
 * @param operator 操作人
 * @param startedAt 开始时间
 * @param finishedAt 结束时间
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record ImportTaskResponse(Long id, String taskNo, String importType, String mode, String onError, String status,
        Integer totalRows, Integer successRows, Integer failRows, String fileRef, String operator,
        LocalDateTime startedAt, LocalDateTime finishedAt) {
}
