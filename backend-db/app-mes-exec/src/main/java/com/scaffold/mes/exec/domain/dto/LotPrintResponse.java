package com.scaffold.mes.exec.domain.dto;

import java.time.LocalDateTime;

/**
 * LotPrintResponse 一条 Lot 打印记录
 *
 * @param tplId 模板
 * @param copies 份数
 * @param reprint 1 表示重打
 * @param printedAt 打印时间
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record LotPrintResponse(Long tplId, int copies, int reprint, LocalDateTime printedAt) {
}
